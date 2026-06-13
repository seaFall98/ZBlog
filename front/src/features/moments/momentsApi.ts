import { apiClient } from "../../lib/apiClient";
import { normalizeMediaUrl } from "../../lib/mediaUrl";
import type { MomentView } from "./types";

type RawRecord = Record<string, unknown>;

type PageResponse = {
  list?: unknown;
  total?: unknown;
  page?: unknown;
  page_size?: unknown;
  pageSize?: unknown;
};

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map(stringValue).map((item) => item.trim()).filter(Boolean);
}

function tagsFrom(value: unknown): string[] {
  if (Array.isArray(value)) return stringArray(value);
  return stringValue(value).split(/[，,\s]+/).map((item) => item.trim()).filter(Boolean);
}

function linkFrom(value: unknown): MomentView["link"] {
  if (!isRecord(value)) return null;
  const url = stringValue(value.url).trim();
  if (!url) return null;
  const favicon = stringValue(value.favicon).trim();
  return {
    url,
    title: stringValue(value.title).trim() || url,
    ...(favicon ? { favicon } : {}),
  };
}

function contentOf(record: RawRecord): RawRecord {
  return isRecord(record.content) ? record.content : record;
}

export function mapMoment(value: unknown): MomentView | null {
  if (!isRecord(value)) return null;
  const content = contentOf(value);
  const text = stringValue(content.text ?? content.content ?? content.body).trim();
  if (!text) return null;
  const tags = tagsFrom(content.tags);
  return {
    id: stringValue(value.id ?? content.id),
    text,
    images: stringArray(content.images).map(normalizeMediaUrl),
    date: stringValue(value.publish_time ?? value.created_at ?? content.date),
    mood: stringValue(content.mood).trim() || tags[0] || "平静",
    tags,
    location: stringValue(content.location).trim(),
    link: linkFrom(content.link),
  };
}

export type MomentListResult = {
  moments: MomentView[];
  total: number;
  page: number;
  pageSize: number;
};

export async function fetchMoments(page = 1, pageSize = 30): Promise<MomentListResult> {
  const data = await apiClient.get<PageResponse>("/moments", { page, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  const moments = list.map(mapMoment).filter((moment): moment is MomentView => Boolean(moment));
  return {
    moments,
    total: Number(data.total) || moments.length,
    page: Number(data.page) || page,
    pageSize: Number(data.page_size ?? data.pageSize) || pageSize,
  };
}
