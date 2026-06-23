import { escapeHtml, estimateReadTime, slugFromText, stripHtml } from "../../lib/text";
import { extractMarkdownToc } from "./toc";
import { isRecord, stringValue as toStringValue, type RawRecord } from "../../lib/typeGuards";
import type { DataSource, PostView, TaxonomyView } from "./types";

function firstValue(record: RawRecord, keys: string[]): unknown {
  return keys.map((key) => record[key]).find((value) => value !== undefined && value !== null && value !== "");
}

function toBooleanValue(value: unknown): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "number") return value !== 0;
  if (typeof value === "string") return ["true", "1", "yes", "y"].includes(value.toLowerCase());
  return false;
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

function textToParagraphHtml(text: string): string {
  const escaped = escapeHtml(text).replace(/\r?\n/g, "<br />");
  return escaped ? `<p>${escaped}</p>` : "";
}

function mapCategory(rawCategory: unknown, article: RawRecord): TaxonomyView | null {
  if (isRecord(rawCategory)) {
    const name = toStringValue(firstValue(rawCategory, ["name", "title", "category_name", "categoryName"]));
    const explicitSlug = toStringValue(firstValue(rawCategory, ["slug"]));
    const urlSlug = lastUrlSegment(firstValue(rawCategory, ["url", "link", "path"]));
    const code = toStringValue(firstValue(rawCategory, ["code"]));
    const slug = explicitSlug || urlSlug || code || slugFromText(name);
    const id = toStringValue(firstValue(rawCategory, ["id", "slug", "code", "name"])) || slug;
    return name ? { id, slug, name } : null;
  }

  const categoryName = toStringValue(firstValue(article, ["category_name", "categoryName"])) || toStringValue(rawCategory);
  if (!categoryName) return null;
  const slug = slugFromText(categoryName);
  return { id: slug, slug, name: categoryName };
}

function mapTag(value: unknown): TaxonomyView | null {
  if (isRecord(value)) {
    const name = toStringValue(firstValue(value, ["name", "title", "tag_name", "tagName"]));
    const explicitSlug = toStringValue(firstValue(value, ["slug"]));
    const urlSlug = lastUrlSegment(firstValue(value, ["url", "link", "path"]));
    const code = toStringValue(firstValue(value, ["code"]));
    const rawId = toStringValue(firstValue(value, ["id"]));
    const slug = explicitSlug || urlSlug || code || (!isPureNumeric(rawId) ? rawId : "") || slugFromText(name);
    const id = !isPureNumeric(rawId) && rawId ? rawId : slug;
    return name ? { id, slug, name } : null;
  }

  const name = toStringValue(value);
  if (!name) return null;
  const slug = slugFromText(name);
  return { id: slug, slug, name };
}

export function mapTags(rawTags: unknown): TaxonomyView[] {
  if (!Array.isArray(rawTags)) return [];
  return rawTags.map(mapTag).filter((tag): tag is TaxonomyView => Boolean(tag));
}

export function mapArticleToPostView(article: RawRecord, source: DataSource = "api"): PostView {
  const id = toStringValue(firstValue(article, ["id", "slug", "title"]));
  const title = toStringValue(firstValue(article, ["title", "name"]));
  const slug = toStringValue(firstValue(article, ["slug", "id"])) || slugFromText(title);
  const html = toStringValue(firstValue(article, ["content_html", "contentHtml", "content"]));
  const contentMarkdown = toStringValue(firstValue(article, ["content_markdown", "contentMarkdown"]));
  const text = toStringValue(firstValue(article, ["content_text", "contentText"])) || contentMarkdown;
  const contentHtml = html || textToParagraphHtml(text);
  const summary =
    toStringValue(firstValue(article, ["summary", "excerpt", "description"])) ||
    stripHtml(contentHtml).slice(0, 120);
  const publishedAt = toStringValue(
    firstValue(article, [
      "publish_time",
      "publishTime",
      "published_at",
      "publishedAt",
      "created_at",
      "createdAt",
      "create_time",
      "createTime",
      "date",
    ]),
  );
  const updatedAt =
    toStringValue(
      firstValue(article, [
        "update_time",
        "updateTime",
        "updated_at",
        "updatedAt",
        "modified_at",
        "modifiedAt",
      ]),
    ) || publishedAt;
  const coverUrl = toStringValue(firstValue(article, ["cover", "cover_url", "coverUrl", "cover_image", "coverImage"]));
  const isTop = toBooleanValue(firstValue(article, ["is_top", "isTop"]));
  const isEssence = toBooleanValue(firstValue(article, ["is_essence", "isEssence"]));
  const featured = toBooleanValue(firstValue(article, ["featured"])) || isTop || isEssence;
  const readTimeValue = firstValue(article, ["readTime", "read_time", "reading_time", "readingTime"]);
  const readTime = Number(readTimeValue) || estimateReadTime(contentHtml || summary || title);
  const viewCount = Number(firstValue(article, ["view_count", "viewCount", "views"])) || 0;
  const rawCopyrightType = toStringValue(firstValue(article, ["copyright_type", "copyrightType"])).toUpperCase();
  const copyrightType =
    rawCopyrightType === "REPOST" || rawCopyrightType === "TRANSLATION" ? rawCopyrightType : "ORIGINAL";

  return {
    id: id || slug,
    slug,
    title,
    summary,
    contentHtml,
    contentMarkdown,
    toc: extractMarkdownToc(contentMarkdown),
    category: mapCategory(firstValue(article, ["category"]), article),
    tags: mapTags(firstValue(article, ["tags"])),
    publishedAt,
    updatedAt,
    coverUrl,
    readTime,
    viewCount,
    isTop,
    copyrightType,
    sourceUrl: toStringValue(firstValue(article, ["source_url", "sourceUrl"])),
    sourceTitle: toStringValue(firstValue(article, ["source_title", "sourceTitle"])),
    copyrightLicense: toStringValue(firstValue(article, ["copyright_license", "copyrightLicense"])),
    featured,
    source,
  };
}

export function mapArticlesToPostViews(articles: unknown, source: DataSource = "api"): PostView[] {
  if (!Array.isArray(articles)) return [];
  return articles.filter(isRecord).map((article) => mapArticleToPostView(article, source));
}
