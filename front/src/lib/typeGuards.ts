export type RawRecord = Record<string, unknown>;

export function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

export function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

export function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map(stringValue).map((s) => s.trim()).filter(Boolean);
}
