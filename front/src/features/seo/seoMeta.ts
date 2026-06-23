type ArticleSeoInput = {
  title: string;
  summary?: string;
  slug: string;
  coverUrl?: string;
  publishedAt?: string;
  updatedAt?: string;
};

export type PageSeo = {
  title: string;
  description: string;
  canonicalUrl: string;
  openGraph?: {
    type: "website" | "article";
    title: string;
    description: string;
    url: string;
    image?: string;
  };
  twitter?: {
    card: "summary" | "summary_large_image";
    title: string;
    description: string;
    image?: string;
  };
};

type PageSeoInput = {
  siteTitle?: string;
  siteDescription?: string;
  pageTitle?: string;
  description?: string;
  path: string;
  origin?: string;
};

type ArticleSeoOptions = {
  siteTitle?: string;
  origin?: string;
  article: ArticleSeoInput;
};

const FALLBACK_SITE_TITLE = "寂静之书";
const FALLBACK_DESCRIPTION = "记录平凡生活里的光与影，写作是一种安静的对话。";

function cleanText(value?: string) {
  return (value ?? "").replace(/\s+/g, " ").trim();
}

function limitDescription(value?: string) {
  const text = cleanText(value) || FALLBACK_DESCRIPTION;
  return text.length > 120 ? text.slice(0, 119) + "…" : text;
}

function absoluteUrl(pathOrUrl: string | undefined, origin?: string) {
  const value = cleanText(pathOrUrl);
  if (!value) return cleanText(origin) || "/";
  if (/^https?:\/\//i.test(value)) return value;
  const base = cleanText(origin).replace(/\/+$/, "");
  if (!base) return value.startsWith("/") ? value : "/" + value;
  return base + (value.startsWith("/") ? value : "/" + value);
}

function titleWithSite(pageTitle: string | undefined, siteTitle: string | undefined) {
  const site = cleanText(siteTitle) || FALLBACK_SITE_TITLE;
  const page = cleanText(pageTitle);
  return page && page !== site ? page + "｜" + site : site;
}

export function buildPageSeo(input: PageSeoInput): PageSeo {
  const description = limitDescription(input.description || input.siteDescription);
  return {
    title: titleWithSite(input.pageTitle, input.siteTitle),
    description,
    canonicalUrl: absoluteUrl(input.path, input.origin),
  };
}

export function buildArticleSeo(input: ArticleSeoOptions): PageSeo {
  const siteTitle = cleanText(input.siteTitle) || FALLBACK_SITE_TITLE;
  const article = input.article;
  const description = limitDescription(article.summary);
  const canonicalUrl = absoluteUrl("/posts/" + article.slug, input.origin);
  const image = article.coverUrl ? absoluteUrl(article.coverUrl, input.origin) : undefined;

  return {
    title: titleWithSite(article.title, siteTitle),
    description,
    canonicalUrl,
    openGraph: {
      type: "article",
      title: article.title,
      description,
      url: canonicalUrl,
      ...(image ? { image } : {}),
    },
    twitter: {
      card: image ? "summary_large_image" : "summary",
      title: article.title,
      description,
      ...(image ? { image } : {}),
    },
  };
}

export function buildArticleJsonLd(input: ArticleSeoOptions) {
  const article = input.article;
  const url = absoluteUrl("/posts/" + article.slug, input.origin);
  const image = article.coverUrl ? absoluteUrl(article.coverUrl, input.origin) : undefined;

  return {
    "@context": "https://schema.org",
    "@type": "Article",
    headline: article.title,
    description: limitDescription(article.summary),
    ...(image ? { image: [image] } : {}),
    ...(article.publishedAt ? { datePublished: article.publishedAt } : {}),
    ...(article.updatedAt ? { dateModified: article.updatedAt } : {}),
    mainEntityOfPage: url,
    publisher: {
      "@type": "Organization",
      name: cleanText(input.siteTitle) || FALLBACK_SITE_TITLE,
    },
  };
}
