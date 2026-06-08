export type ApiEnvelope<T> = {
  code: number;
  message: string;
  data?: T;
};

export type PageResponse<T> = {
  list?: T[];
  total?: number;
  page?: number;
  page_size?: number;
  pageSize?: number;
};

export class ApiEnvelopeError extends Error {
  readonly code: number;

  constructor(code: number, message: string) {
    super(message || `API request failed with code ${code}`);
    this.name = "ApiEnvelopeError";
    this.code = code;
  }
}

export function isApiEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  if (!value || typeof value !== "object") return false;
  const candidate = value as Record<string, unknown>;
  return typeof candidate.code === "number" && typeof candidate.message === "string";
}

export function parseApiEnvelope<T>(value: unknown): T {
  if (!isApiEnvelope(value)) {
    throw new Error("Invalid API response envelope");
  }

  if (value.code !== 0) {
    throw new ApiEnvelopeError(value.code, value.message);
  }

  return value.data as T;
}
