import { posts } from "../../data/mockData";
import { mapArticleToPostView } from "./blogMapper";
import type { PostFilterParams, PostListResult, PostView } from "./types";

export const categorySlugByName: Record<string, string> = {
  写作: "writing",
  阅读: "reading",
  旅行: "travel",
  生活: "life",
  影像: "film",
  随想: "thoughts",
};

export const fallbackPosts: PostView[] = posts.map((post) =>
  mapArticleToPostView(
    {
      id: post.id,
      slug: post.id,
      title: post.title,
      summary: post.excerpt,
      content_html: post.content,
      category: {
        id: categorySlugByName[post.category] ?? post.category,
        slug: categorySlugByName[post.category] ?? post.category,
        name: post.category,
      },
      tags: post.tags,
      publish_time: post.date,
      cover_url: post.coverImage,
      readTime: post.readTime,
      featured: post.featured,
    },
    "fallback",
  ),
);

export function findFallbackPostBySlug(slug: string): PostView | undefined {
  return fallbackPosts.find((post) => post.slug === slug || post.id === slug);
}

function matchesFilter(post: PostView, params: PostFilterParams): boolean {
  if (params.category) {
    const category = params.category.toLowerCase();
    const matchesCategory =
      post.category?.slug.toLowerCase() === category || post.category?.name.toLowerCase() === category;
    if (!matchesCategory) return false;
  }

  if (params.tag) {
    const tag = params.tag.toLowerCase();
    if (!post.tags.some((item) => item.slug.toLowerCase() === tag || item.name.toLowerCase() === tag)) return false;
  }

  if (params.year) {
    const year = String(params.year).padStart(4, "0");
    if (!post.publishedAt.startsWith(year)) return false;
  }

  if (params.month) {
    const month = String(params.month).padStart(2, "0");
    const postMonth = post.publishedAt.match(/^\d{4}-(\d{2})/)?.[1];
    if (postMonth !== month) return false;
  }

  if (params.keyword) {
    const keyword = params.keyword.toLowerCase().trim();
    if (keyword) {
      const haystack = [
        post.title,
        post.summary,
        post.category?.name ?? "",
        ...post.tags.map((tag) => tag.name),
        post.contentHtml,
      ]
        .join(" ")
        .toLowerCase();
      if (!haystack.includes(keyword)) return false;
    }
  }

  return true;
}

function filterFallbackPosts(params: PostFilterParams = {}): PostView[] {
  return fallbackPosts.filter((post) => matchesFilter(post, params));
}

export function getFallbackPosts(params: PostFilterParams = {}): PostListResult {
  const page = Number(params.page) || 1;
  const pageSize = Number(params.pageSize) || fallbackPosts.length;
  const filtered = filterFallbackPosts(params);
  const start = (page - 1) * pageSize;
  const paged = filtered.slice(start, start + pageSize);

  return {
    posts: paged,
    total: filtered.length,
    page,
    pageSize,
    source: "fallback",
  };
}

function timeValue(post: PostView): number {
  const value = Date.parse(post.publishedAt);
  return Number.isNaN(value) ? Number.NEGATIVE_INFINITY : value;
}

export function mergeFallbackAndApiPosts(apiResult: PostListResult, params: PostFilterParams = {}): PostListResult {
  const page = Number(params.page) || apiResult.page || 1;
  if (page > 1) return apiResult;

  const apiPosts = apiResult.posts.filter((post) => matchesFilter(post, params));
  const pageSize = apiResult.pageSize || Number(params.pageSize) || apiPosts.length || fallbackPosts.length;
  const apiSlugs = new Set(apiPosts.map((post) => post.slug));
  const fallbackSlots = Math.max(pageSize - apiPosts.length, 0);
  const fallbackForMerge = filterFallbackPosts(params)
    .filter((post) => !apiSlugs.has(post.slug))
    .slice(0, fallbackSlots);
  const postsForMerge = apiPosts.length > 0 ? [...fallbackForMerge, ...apiPosts] : filterFallbackPosts(params).slice(0, pageSize);
  const orderBySlug = new Map<string, number>();

  postsForMerge.forEach((post, index) => {
    orderBySlug.set(post.slug, index);
  });

  const merged = postsForMerge.sort((left, right) => {
    const dateOrder = timeValue(right) - timeValue(left);
    if (dateOrder !== 0) return dateOrder;
    return (orderBySlug.get(left.slug) ?? 0) - (orderBySlug.get(right.slug) ?? 0);
  });
  const hasApiPosts = apiPosts.length > 0;

  return {
    posts: merged,
    total: hasApiPosts ? apiResult.total : filterFallbackPosts(params).length,
    page: apiResult.page || page,
    pageSize,
    source: hasApiPosts ? "api" : "fallback",
  };
}

export function getRelatedFallbackPosts(post: PostView, limit = 3): PostView[] {
  return fallbackPosts
    .filter((candidate) => candidate.slug !== post.slug)
    .filter((candidate) => {
      const sameCategory = candidate.category?.slug === post.category?.slug;
      const sameTag = candidate.tags.some((tag) => post.tags.some((postTag) => postTag.slug === tag.slug));
      return sameCategory || sameTag;
    })
    .slice(0, limit);
}
