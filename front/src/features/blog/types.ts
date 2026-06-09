import type { TocItem } from "./toc";

export type DataSource = "api";

export type TaxonomyView = {
  id: string;
  slug: string;
  name: string;
};

export type PostView = {
  id: string;
  slug: string;
  title: string;
  summary: string;
  contentHtml: string;
  contentMarkdown: string;
  toc: TocItem[];
  category: TaxonomyView | null;
  tags: TaxonomyView[];
  publishedAt: string;
  coverUrl: string;
  readTime: number;
  featured: boolean;
  source: DataSource;
};

export type PostListResult = {
  posts: PostView[];
  total: number;
  page: number;
  pageSize: number;
  source: DataSource;
};

export type ArticleListParams = {
  page?: number;
  pageSize?: number;
  category?: string;
  tag?: string;
  year?: number | string;
  month?: number | string;
};

export type ArticleSearchParams = ArticleListParams & {
  keyword?: string;
};

export type PostFilterParams = ArticleSearchParams;
