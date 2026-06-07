export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function stripHtml(html: string): string {
  return html.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim();
}

export function truncateText(text: string, maxLength: number): string {
  const normalized = text.trim();
  if (normalized.length <= maxLength) return normalized;
  return `${normalized.slice(0, maxLength)}…`;
}

export function estimateReadTime(text: string): number {
  const plain = stripHtml(text);
  const cjkCount = (plain.match(/[一-鿿]/g) ?? []).length;
  const nonCjkText = plain.replace(/[一-鿿]/g, " ");
  const wordCount = nonCjkText.split(/\s+/).filter(Boolean).length;
  const units = cjkCount / 400 + wordCount / 200;
  return Math.max(1, Math.ceil(units));
}

const projectLocalDateFormatter = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Shanghai",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

function formatProjectLocalDate(date: Date): string {
  const parts = projectLocalDateFormatter.formatToParts(date);
  const year = parts.find((part) => part.type === "year")?.value;
  const month = parts.find((part) => part.type === "month")?.value;
  const day = parts.find((part) => part.type === "day")?.value;
  return year && month && day ? `${year}-${month}-${day}` : date.toISOString().slice(0, 10);
}

export function toDateText(value: string): string {
  if (!value) return "未发布";
  if (/^\d{4}-\d{2}-\d{2}T.+Z$/i.test(value)) {
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) return formatProjectLocalDate(date);
  }
  const inputDate = value.match(/^(\d{4}-\d{2}-\d{2})(?:[T\s]|$)/)?.[1];
  if (inputDate) return inputDate;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.slice(0, 10);
  return date.toISOString().slice(0, 10);
}

export function slugFromText(text: string): string {
  const slug = text
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9一-鿿]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return slug || "untitled";
}
