import { Helmet } from "react-helmet-async";
import { useLocation } from "react-router-dom";
import { useSiteProfile } from "../site/useSiteProfile";
import type { PostView } from "../blog/types";
import { buildArticleJsonLd, buildArticleSeo, buildPageSeo, type PageSeo } from "./seoMeta";

function browserOrigin() {
  return typeof window === "undefined" ? "" : window.location.origin;
}

function siteDescription(profile: ReturnType<typeof useSiteProfile>["profile"]) {
  return profile.footerDescription || profile.subtitle || profile.heroSlogan || profile.aboutIntro;
}

function feedUrl(path: string) {
  return browserOrigin().replace(/\/+$/, "") + path;
}

function SeoTags({ seo, jsonLd }: { seo: PageSeo; jsonLd?: object }) {
  return (
    <Helmet>
      <title>{seo.title}</title>
      <meta name="description" content={seo.description} />
      <link rel="canonical" href={seo.canonicalUrl} />
      <link rel="alternate" type="application/rss+xml" title="RSS" href={feedUrl("/rss.xml")} />
      <link rel="alternate" type="application/atom+xml" title="Atom" href={feedUrl("/atom.xml")} />
      <meta property="og:title" content={seo.openGraph?.title ?? seo.title} />
      <meta property="og:description" content={seo.openGraph?.description ?? seo.description} />
      <meta property="og:type" content={seo.openGraph?.type ?? "website"} />
      <meta property="og:url" content={seo.openGraph?.url ?? seo.canonicalUrl} />
      {seo.openGraph?.image && <meta property="og:image" content={seo.openGraph.image} />}
      <meta name="twitter:card" content={seo.twitter?.card ?? "summary"} />
      <meta name="twitter:title" content={seo.twitter?.title ?? seo.title} />
      <meta name="twitter:description" content={seo.twitter?.description ?? seo.description} />
      {seo.twitter?.image && <meta name="twitter:image" content={seo.twitter.image} />}
      {jsonLd && <script type="application/ld+json">{JSON.stringify(jsonLd)}</script>}
    </Helmet>
  );
}

export function PageSeoHead({
  pageTitle,
  description,
  path,
  home = false,
}: {
  pageTitle?: string;
  description?: string;
  path: string;
  home?: boolean;
}) {
  const { profile } = useSiteProfile();
  const baseDescription = description || siteDescription(profile);
  const seo = buildPageSeo({
    siteTitle: profile.title,
    siteDescription: baseDescription,
    pageTitle,
    description: baseDescription,
    path,
    origin: browserOrigin(),
  });

  if (home && profile.title && baseDescription) {
    seo.title = profile.title + "｜" + baseDescription;
  }

  return <SeoTags seo={seo} />;
}

export function ArticleSeoHead({ post }: { post: PostView }) {
  const { profile } = useSiteProfile();
  const article = {
    title: post.title,
    summary: post.summary,
    slug: post.slug,
    coverUrl: post.coverUrl || profile.avatarUrl || profile.backgroundImage,
    publishedAt: post.publishedAt,
    updatedAt: post.updatedAt || post.publishedAt,
  };
  const seo = buildArticleSeo({
    siteTitle: profile.title,
    origin: browserOrigin(),
    article,
  });
  const jsonLd = buildArticleJsonLd({
    siteTitle: profile.title,
    origin: browserOrigin(),
    article,
  });

  return <SeoTags seo={seo} jsonLd={jsonLd} />;
}

function routeTitle(pathname: string) {
  if (pathname === "/") return { title: undefined, home: true };
  if (pathname === "/blog") return { title: "文章" };
  if (pathname.startsWith("/category/")) return { title: "分类：" + decodeURIComponent(pathname.split("/").pop() || "") };
  if (pathname.startsWith("/tag/")) return { title: "标签：" + decodeURIComponent(pathname.split("/").pop() || "") };
  if (pathname === "/categories") return { title: "分类" };
  if (pathname === "/tags") return { title: "标签" };
  if (pathname === "/archive" || pathname.startsWith("/archive/")) return { title: "归档" };
  if (pathname === "/search") return { title: "搜索" };
  if (pathname === "/gallery") return { title: "相册" };
  if (pathname.startsWith("/gallery/")) return { title: "相册详情" };
  if (pathname === "/moments") return { title: "瞬间" };
  if (pathname === "/guestbook" || pathname === "/message") return { title: "留言墙" };
  if (pathname === "/links") return { title: "友情链接" };
  if (pathname === "/about") return { title: "关于" };
  if (pathname === "/privacy") return { title: "隐私政策" };
  if (pathname === "/cookies") return { title: "Cookies" };
  if (pathname === "/copyright") return { title: "版权说明" };
  if (pathname === "/feedback") return { title: "反馈" };
  if (pathname === "/feedback/mine") return { title: "我的反馈" };
  if (pathname === "/stats" || pathname === "/statistics") return { title: "站点统计" };
  if (pathname === "/login" || pathname === "/register" || pathname === "/forgot-password") return { title: "账号" };
  if (pathname === "/profile") return { title: "个人中心" };
  if (pathname === "/notifications") return { title: "通知" };
  if (pathname.startsWith("/posts/") || pathname.startsWith("/blog/")) return { title: "文章" };
  return { title: "未找到页面" };
}

export function RouteSeoDefaults() {
  const location = useLocation();
  const route = routeTitle(location.pathname);
  return <PageSeoHead pageTitle={route.title} path={location.pathname} home={route.home} />;
}
