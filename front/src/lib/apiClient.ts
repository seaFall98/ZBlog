import { ApiEnvelopeError, isApiEnvelope, parseApiEnvelope } from "./apiEnvelope";

export type ApiQuery = Record<string, string | number | boolean | null | undefined>;

export type ApiClientOptions = {
  baseUrl?: string;
  fetcher?: typeof fetch;
};

export class ApiHttpError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiHttpError";
    this.status = status;
  }
}

function trimRightSlash(value: string): string {
  return value.replace(/\/+$/, "");
}

function normalizePath(path: string): string {
  return path.startsWith("/") ? path : `/${path}`;
}

async function tryReadJson(response: Response): Promise<unknown | undefined> {
  try {
    return (await response.json()) as unknown;
  } catch {
    return undefined;
  }
}

export function buildApiUrl(baseUrl: string, path: string, query?: ApiQuery): string {
  const search = new URLSearchParams();
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });

  const url = `${trimRightSlash(baseUrl)}${normalizePath(path)}`;
  const queryString = search.toString();
  return queryString ? `${url}?${queryString}` : url;
}

export function createApiClient(options: ApiClientOptions = {}) {
  const baseUrl = options.baseUrl ?? import.meta.env.VITE_ZBLOG_API_BASE_URL ?? "/api/v1";
  const fetcher = options.fetcher ?? fetch;

  return {
    async get<T>(path: string, query?: ApiQuery): Promise<T> {
      const response = await fetcher(buildApiUrl(baseUrl, path, query), {
        headers: { Accept: "application/json" },
      });

      const json = response.ok ? ((await response.json()) as unknown) : await tryReadJson(response);

      if (!response.ok) {
        if (isApiEnvelope(json) && json.code !== 0) {
          throw new ApiEnvelopeError(json.code, json.message);
        }

        throw new ApiHttpError(response.status, `HTTP ${response.status}`);
      }

      return parseApiEnvelope<T>(json);
    },

    async post<T>(path: string, body?: unknown): Promise<T> {
      const response = await fetcher(buildApiUrl(baseUrl, path), {
        method: "POST",
        headers: { Accept: "application/json", "Content-Type": "application/json" },
        body: JSON.stringify(body ?? {}),
      });

      const json = response.ok ? ((await response.json()) as unknown) : await tryReadJson(response);

      if (!response.ok) {
        if (isApiEnvelope(json) && json.code !== 0) {
          throw new ApiEnvelopeError(json.code, json.message);
        }

        throw new ApiHttpError(response.status, `HTTP ${response.status}`);
      }

      return parseApiEnvelope<T>(json);
    },
  };
}

export const apiClient = createApiClient();
