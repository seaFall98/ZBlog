import { useEffect, useState } from "react";
import { blogApi } from "./blogApi";
import { getFallbackPosts, mergeFallbackAndApiPosts } from "./blogFallback";
import type { PostFilterParams, PostListResult } from "./types";

type UsePostsState = PostListResult & {
  loading: boolean;
  error: unknown;
};

type UsePostsOptions = {
  initialFallback?: boolean;
};

function emptyPosts(params: PostFilterParams = {}): PostListResult {
  return {
    posts: [],
    total: 0,
    page: Number(params.page) || 1,
    pageSize: Number(params.pageSize) || 0,
    source: "fallback",
  };
}

export function usePosts(params: PostFilterParams = {}, options: UsePostsOptions = {}): UsePostsState {
  const { initialFallback = true } = options;
  const { category, tag, year, month, keyword, page, pageSize } = params;
  const [state, setState] = useState<UsePostsState>(() => ({
    ...(initialFallback
      ? getFallbackPosts({ category, tag, year, month, keyword, page, pageSize })
      : emptyPosts({ category, tag, year, month, keyword, page, pageSize })),
    loading: true,
    error: null,
  }));

  useEffect(() => {
    const requestParams: PostFilterParams = { category, tag, year, month, keyword, page, pageSize };
    let active = true;

    async function load() {
      setState((current) => ({
        ...(initialFallback ? current : emptyPosts(requestParams)),
        loading: true,
        error: null,
      }));
      try {
        const result = keyword ? await blogApi.searchPosts(requestParams) : await blogApi.listPosts(requestParams);

        if (!active) return;
        setState({ ...mergeFallbackAndApiPosts(result, requestParams), loading: false, error: null });
      } catch (error) {
        if (!active) return;
        setState({ ...getFallbackPosts(requestParams), loading: false, error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [category, tag, year, month, keyword, page, pageSize, initialFallback]);

  return state;
}
