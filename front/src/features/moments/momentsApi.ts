import { apiClient } from "../../lib/apiClient";
import type { MomentView } from "./types";

type RawRecord = Record<string, unknown>;

type PageResponse = {
  list?: unknown;
};

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map(stringValue).filter(Boolean);
}

function contentOf(record: RawRecord): RawRecord {
  return isRecord(record.content) ? record.content : record;
}

function mapMoment(value: unknown): MomentView | null {
  if (!isRecord(value)) return null;
  const content = contentOf(value);
  const text = stringValue(content.text ?? content.content ?? content.body);
  if (!text) return null;
  return {
    id: stringValue(value.id ?? content.id),
    text,
    images: stringArray(content.images),
    date: stringValue(value.publish_time ?? value.created_at ?? content.date),
    mood: stringValue(content.mood) || "平静",
  };
}

export async function fetchMoments(pageSize = 30): Promise<MomentView[]> {
  const data = await apiClient.get<PageResponse>("/moments", { page: 1, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  return list.map(mapMoment).filter((moment): moment is MomentView => Boolean(moment));
}
