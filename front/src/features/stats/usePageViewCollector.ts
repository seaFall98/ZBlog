import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { collect } from "./collectApi";

const PRIVATE_PREFIXES = ["/login", "/register", "/forgot-password", "/profile", "/notifications", "/feedback/mine"];
const ARTICLE_PREFIXES = ["/posts"];
const REDIRECT_PATHS = ["/message", "/statistics", "/blog/detail", "/gallery/detail"];

type PageViewCollectorOptions = {
  onArticleViewCount?: (viewCount: number) => void;
};

function isArticlePath(pathname: string) {
  return ARTICLE_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)) || /^\/blog\/[^/]+/.test(pathname);
}

function shouldCollect(pathname: string, hasArticleId: boolean) {
  if (REDIRECT_PATHS.includes(pathname)) {
    return false;
  }
  if (isArticlePath(pathname) && !hasArticleId) {
    return false;
  }
  return !PRIVATE_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));
}

function screenValue() {
  if (typeof window === "undefined") return "";
  return `${window.screen.width}x${window.screen.height}@${window.devicePixelRatio || 1}`;
}

export function usePageViewCollector(articleId?: string | number | null, options: PageViewCollectorOptions = {}) {
  const location = useLocation();
  const { onArticleViewCount } = options;

  useEffect(() => {
    const numericArticleId = articleId == null || articleId === "" ? undefined : Number(articleId);
    const hasArticleId = Number.isFinite(numericArticleId);

    if (typeof window === "undefined" || !shouldCollect(location.pathname, hasArticleId)) {
      return;
    }

    const payload = {
      type: "pageview" as const,
      url: `${location.pathname}${location.search}`,
      hostname: window.location.hostname,
      title: document.title || "",
      referrer: document.referrer || "",
      language: navigator.language || "",
      screen: screenValue(),
      article_id: Number.isFinite(numericArticleId) ? numericArticleId : undefined,
      timestamp: Date.now(),
    };

    collect(payload)
      .then((response) => {
        if (typeof response.article_view_count === "number") {
          onArticleViewCount?.(response.article_view_count);
        }
      })
      .catch(() => {
        // Analytics should never interrupt reading.
      });
  }, [articleId, location.pathname, location.search, onArticleViewCount]);
}
