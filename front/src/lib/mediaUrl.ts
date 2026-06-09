export function normalizeMediaUrl(value: string | null | undefined): string {
  const url = value?.trim() ?? "";
  if (!url) return "";
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith("/")) return url;
  return `/${url}`;
}
