import { createApiClient, type ApiClientOptions, type ApiQuery } from "../../lib/apiClient";
import type { PageResponse } from "../../lib/apiEnvelope";
import { mapArticleToPostView, mapArticlesToPostViews } from "./blogMapper";
import type { ArticleListParams, ArticleSearchParams, PostListResult } from "./types";

type ArticleListResponse = PageResponse<unknown> | unknown[];

function toPageQuery(params: ArticleListParams = {}): ApiQuery {
  return {
    page: params.page,
    page_size: params.pageSize,
    category: params.category,
    tag: params.tag,
    year: params.year,
    month: params.month,
  };
}

function toPostListResult(data: ArticleListResponse, fallbackPage = 1, fallbackPageSize = 10): PostListResult {
  if (Array.isArray(data)) {
    const posts = mapArticlesToPostViews(data);
    return {
      posts,
      total: posts.length,
      page: fallbackPage,
      pageSize: fallbackPageSize,
      source: "api",
    };
  }

  const posts = mapArticlesToPostViews(data?.list);
  return {
    posts,
    total: Number(data?.total) || posts.length,
    page: Number(data?.page) || fallbackPage,
    pageSize: Number(data?.page_size ?? data?.pageSize) || fallbackPageSize,
    source: "api",
  };
}

export function createBlogApi(options: ApiClientOptions = {}) {
  const client = createApiClient(options);

  return {
    async listPosts(params: ArticleListParams = {}): Promise<PostListResult> {
      const data = await client.get<ArticleListResponse>("/articles", toPageQuery(params));
      return toPostListResult(data, Number(params.page) || 1, Number(params.pageSize) || 10);
    },

    async getPost(slug: string) {
      const data = await client.get<Record<string, unknown>>(`/articles/${encodeURIComponent(slug)}`);
      return mapArticleToPostView(data);
    },

    async searchPosts(params: ArticleSearchParams): Promise<PostListResult> {
      const data = await client.get<ArticleListResponse>("/articles/search", {
        keyword: params.keyword,
        ...toPageQuery(params),
      });
      return toPostListResult(data, Number(params.page) || 1, Number(params.pageSize) || 50);
    },

    async listHotPosts(type: "recent" | "total", limit = 20): Promise<PostListResult> {
      const data = await client.get<ArticleListResponse>("/articles/hot", { type, limit });
      return toPostListResult(data, 1, limit);
    },
  };
}

export const blogApi = createBlogApi();
