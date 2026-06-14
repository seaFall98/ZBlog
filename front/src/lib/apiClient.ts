import { ApiEnvelopeError, isApiEnvelope, parseApiEnvelope } from "./apiEnvelope";

export type ApiQuery = Record<string, string | number | boolean | null | undefined>;

export type ApiClientOptions = {
  baseUrl?: string;
  fetcher?: typeof fetch;
};

let authTokenProvider: (() => string | null | undefined) | null = null;

export function setApiAuthTokenProvider(provider: (() => string | null | undefined) | null) {
  authTokenProvider = provider;
}

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

  const jsonHeaders = () => {
    const headers: Record<string, string> = { Accept: "application/json" };
    const token = authTokenProvider?.();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    return headers;
  };

  const jsonBodyHeaders = () => ({ ...jsonHeaders(), "Content-Type": "application/json" });

  async function parseResponse<T>(response: Response): Promise<T> {
    const json = response.ok ? ((await response.json()) as unknown) : await tryReadJson(response);

    if (!response.ok) {
      if (response.status === 401 && authTokenProvider?.()) {
        // Only clear session if we sent a token — avoids false positives
        // when a public request legitimately returns 401.
        window.dispatchEvent(new Event("zblog:auth-expired"));
      }
      if (isApiEnvelope(json) && json.code !== 0) {
        throw new ApiEnvelopeError(json.code, json.message);
      }

      throw new ApiHttpError(response.status, `HTTP ${response.status}`);
    }

    return parseApiEnvelope<T>(json);
  }

  async function request<T>(method: string, path: string, body?: unknown, query?: ApiQuery): Promise<T> {
    const init: RequestInit = {
      headers: body === undefined ? jsonHeaders() : jsonBodyHeaders(),
    };
    if (method !== "GET") {
      init.method = method;
    }
    if (body !== undefined) {
      init.body = JSON.stringify(body ?? {});
    }
    const response = await fetcher(buildApiUrl(baseUrl, path, query), init);

    return parseResponse<T>(response);
  }

  return {
    async get<T>(path: string, query?: ApiQuery): Promise<T> {
      return request<T>("GET", path, undefined, query);
    },

    async post<T>(path: string, body?: unknown): Promise<T> {
      return request<T>("POST", path, body);
    },

    async put<T>(path: string, body?: unknown): Promise<T> {
      return request<T>("PUT", path, body);
    },

    async patch<T>(path: string, body?: unknown): Promise<T> {
      return request<T>("PATCH", path, body);
    },

    async delete<T>(path: string, body?: unknown): Promise<T> {
      return request<T>("DELETE", path, body);
    },

    async upload<T>(path: string, body: FormData): Promise<T> {
      const response = await fetcher(buildApiUrl(baseUrl, path), {
        method: "POST",
        headers: jsonHeaders(),
        body,
      });

      return parseResponse<T>(response);
    },
  };
}

export const apiClient = createApiClient();
