const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function normalizeLinkUrl(url: string): string {
  const trimmed = url.trim();
  if (emailPattern.test(trimmed)) {
    return `mailto:${trimmed}`;
  }
  return trimmed;
}

export function isExternalLink(url: string): boolean {
  const normalized = normalizeLinkUrl(url);
  return !normalized.startsWith('/');
}
