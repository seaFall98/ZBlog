import { useQuery } from "@tanstack/react-query";
import { blogApi } from "./blogApi";
import type { PostFilterParams, PostListResult } from "./types";

type UsePostsState = PostListResult & {
  loading: boolean;
  error: unknown;
};

export function usePosts(params: PostFilterParams = {}): UsePostsState {
  const { data, isLoading, error } = useQuery({
    queryKey: ["posts", params],
    queryFn: () =>
      params.keyword ? blogApi.searchPosts(params) : blogApi.listPosts(params),
  });

  return {
    posts: data?.posts ?? [],
    total: data?.total ?? 0,
    page: data?.page ?? (Number(params.page) || 1),
    pageSize: data?.pageSize ?? (Number(params.pageSize) || 20),
    source: data?.source ?? "api",
    loading: isLoading,
    error,
  };
}
