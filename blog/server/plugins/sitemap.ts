// 动态生成 sitemap 路由

interface SitemapArticle {
  url: string;
  title?: string;
  cover?: string;
  update_time?: string;
  publish_time?: string;
}

interface SitemapCategory {
  url: string;
}

interface SitemapTag {
  url: string;
}

interface ArticlesResponse {
  data?: {
    list?: SitemapArticle[];
  };
}

interface CategoriesResponse {
  data?: {
    list?: SitemapCategory[];
  };
}

interface TagsResponse {
  data?: {
    list?: SitemapTag[];
  };
}

interface SitemapImage {
  loc: string;
  caption?: string;
}

interface SitemapUrl {
  loc: string;
  lastmod?: string;
  changefreq: 'weekly';
  priority: number;
  images?: SitemapImage[];
}

/**
 * 将日期字符串转换为 W3C 标准的 ISO 8601 格式
 * @param dateStr - 后端返回的日期字符串 (如 "2026-04-20 12:09:14")
 * @returns ISO 8601 格式的日期字符串 (如 "2026-04-20T12:09:14+00:00")
 */
function formatW3CDate(dateStr: string | undefined): string | undefined {
  if (!dateStr) return undefined;

  // 将 "2026-04-20 12:09:14" 转换为 Date 对象
  const date = new Date(dateStr.replace(' ', 'T'));

  if (isNaN(date.getTime())) return undefined;

  // 返回 ISO 8601 格式，带时区偏移
  return date.toISOString();
}

/**
 * 获取构建时间作为静态页面的 lastmod
 * @returns ISO 8601 格式的构建时间
 */
function getBuildTime(): string {
  // 使用当前时间作为构建时间
  return new Date().toISOString();
}

function normalizeBaseUrl(value: string | undefined): string {
  return (value || '').trim().replace(/\/+$/, '');
}

function absoluteUrl(pathOrUrl: string | undefined, siteUrl: string): string {
  if (!pathOrUrl) return siteUrl || '/';
  if (/^https?:\/\//i.test(pathOrUrl)) return pathOrUrl;
  if (!siteUrl) return pathOrUrl;
  return `${siteUrl}${pathOrUrl.startsWith('/') ? pathOrUrl : `/${pathOrUrl}`}`;
}

/**
 * 判断是否为静态页面（非动态内容页面）
 * @param url - 页面 URL
 * @returns 是否为静态页面
 */
function isStaticPage(url: string): boolean {
  // 静态页面列表
  const staticPages = [
    '/',
    '/about',
    '/friend',
    '/message',
    '/moment',
    '/privacy',
    '/categories',
    '/tags',
    '/archive',
  ];
  return staticPages.includes(url);
}

export default defineNitroPlugin(async nitroApp => {
  nitroApp.hooks.hook('sitemap:resolved', async ctx => {
    try {
      const config = useRuntimeConfig();
      const apiUrl = (config.apiServerUrl || config.public.apiUrl) as string;
      const siteUrl = normalizeBaseUrl(config.public.siteUrl as string | undefined);
      const buildTime = getBuildTime();

      // 从后端 API 获取文章列表
      const articlesRes = await $fetch<ArticlesResponse>(`${apiUrl}/articles`).catch(() => null);

      // 添加文章路由到 sitemap
      const articles = articlesRes?.data?.list || [];
      articles.forEach((article: SitemapArticle) => {
        const sitemapUrl: SitemapUrl = {
          loc: absoluteUrl(article.url, siteUrl),
          lastmod: formatW3CDate(article.update_time || article.publish_time),
          changefreq: 'weekly',
          priority: 0.8,
        };

        // 如果有封面图，添加到图片 sitemap
        if (article.cover) {
          sitemapUrl.images = [
            {
              loc: absoluteUrl(article.cover, siteUrl),
              caption: article.title,
            },
          ];
        }

        ctx.urls.push(sitemapUrl);
      });

      // 添加分类路由
      const categoriesRes = await $fetch<CategoriesResponse>(`${apiUrl}/categories`).catch(
        () => null
      );
      const categories = categoriesRes?.data?.list || [];
      categories.forEach((category: SitemapCategory) => {
        ctx.urls.push({
          loc: absoluteUrl(category.url, siteUrl),
          lastmod: buildTime,
          changefreq: 'weekly',
          priority: 0.6,
        } as SitemapUrl);
      });

      // 添加标签路由
      const tagsRes = await $fetch<TagsResponse>(`${apiUrl}/tags`).catch(() => null);
      const tags = tagsRes?.data?.list || [];
      tags.forEach((tag: SitemapTag) => {
        ctx.urls.push({
          loc: absoluteUrl(tag.url, siteUrl),
          lastmod: buildTime,
          changefreq: 'weekly',
          priority: 0.5,
        } as SitemapUrl);
      });

      // 为已存在的静态页面添加 lastmod（构建时间）
      ctx.urls.forEach((url: SitemapUrl) => {
        const path = siteUrl && url.loc.startsWith(siteUrl) ? url.loc.slice(siteUrl.length) || '/' : url.loc;
        if (isStaticPage(path) && !url.lastmod) {
          url.lastmod = buildTime;
        }
        url.loc = absoluteUrl(url.loc, siteUrl);
      });
    } catch {
      // sitemap 生成失败不影响应用运行，静默忽略
    }
  });
});
