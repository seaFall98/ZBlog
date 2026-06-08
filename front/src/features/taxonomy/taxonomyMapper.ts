import { slugFromText } from "../../lib/text";
import type { TaxonomyItem } from "./types";

type TaxonomyRecord = Record<string, unknown>;

function firstValue(record: TaxonomyRecord, keys: string[]): unknown {
  return keys.map((key) => record[key]).find((value) => value !== undefined && value !== null && value !== "");
}

function toStringValue(value: unknown): string {
  if (value === undefined || value === null) return "";
  return String(value);
}

function toNumberValue(value: unknown): number {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function isPureNumeric(value: string): boolean {
  return /^\d+$/.test(value.trim());
}

function lastUrlSegment(value: unknown): string {
  const url = toStringValue(value).trim().replace(/\/+$/, "");
  if (!url) return "";
  const segment = url.split("/").filter(Boolean).pop() ?? "";
  return decodeURIComponent(segment);
}

export function mapTaxonomyItem(record: TaxonomyRecord): TaxonomyItem {
  const name =
    toStringValue(firstValue(record, ["name", "title", "label", "category_name", "categoryName", "tag_name", "tagName"])).trim() ||
    "未命名";
  const explicitSlug = toStringValue(firstValue(record, ["slug"])).trim();
  const code = toStringValue(firstValue(record, ["code"])).trim();
  const urlSlug = lastUrlSegment(firstValue(record, ["url", "link", "path"]));
  const rawId = toStringValue(firstValue(record, ["id"])).trim();
  const nameSlug = slugFromText(name);
  const slug = explicitSlug || code || urlSlug || (!isPureNumeric(rawId) ? rawId : "") || nameSlug;
  const id = rawId && (explicitSlug || code) ? rawId : !isPureNumeric(rawId) && rawId ? rawId : slug;

  const item: TaxonomyItem = {
    id,
    name,
    slug,
    count: toNumberValue(firstValue(record, ["article_count", "articleCount", "count"])),
  };
  const description = toStringValue(firstValue(record, ["description", "summary"]));
  const coverUrl = toStringValue(firstValue(record, ["cover_url", "coverUrl", "cover_image", "coverImage"]));
  if (description) item.description = description;
  if (coverUrl) item.coverUrl = coverUrl;
  return item;
}

function taxonomyRecordsFromResponse(response: unknown): unknown[] {
  if (Array.isArray(response)) return response;
  if (response && typeof response === "object" && !Array.isArray(response)) {
    const list = (response as TaxonomyRecord).list;
    if (Array.isArray(list)) return list;
  }
  return [];
}

export function mapTaxonomyItems(response: unknown): TaxonomyItem[] {
  return taxonomyRecordsFromResponse(response)
    .filter((record): record is TaxonomyRecord => Boolean(record) && typeof record === "object" && !Array.isArray(record))
    .map(mapTaxonomyItem);
}
