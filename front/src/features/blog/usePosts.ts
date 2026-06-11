import { useQuery } from "@tanstack/react-query";
import { blogApi } from "./blogApi";
import type { PostFilterParams, PostListResult } from "./types";

type UsePostsState = PostListResult & {
  loading: boolean;
  error: unknown;
};

function emptyPosts(params: PostFilterParams = {}): PostListResult {
  return {
    posts: [],
    total: 0,
    page: Number(params.page) || 1,
    pageSize: Number(params.pageSize) || 0,
    source: "api",
  };
}

function getCacheKey(params: PostFilterParams): string {
  return JSON.stringify({
    category: params.category ?? "",
    tag: params.tag ?? "",
    year: params.year ?? "",
    month: params.month ?? "",
    keyword: params.keyword ?? "",
    page: Number(params.page) || 1,
    pageSize: Number(params.pageSize) || 0,
  });
}

export function usePosts(params: PostFilterParams = {}): UsePostsState {
  const { category, tag, year, month, keyword, page, pageSize } = params;
  const requestParams: PostFilterParams = { category, tag, year, month, keyword, page, pageSize };
  const cacheKey = getCacheKey(requestParams);

  const { data, isLoading, error } = useQuery({
    queryKey: ["posts", cacheKey],
    queryFn: () =>
      keyword ? blogApi.searchPosts(requestParams) : blogApi.listPosts(requestParams),
  });

  return {
    ...(data ?? emptyPosts(requestParams)),
    loading: isLoading,
    error,
  };
}
