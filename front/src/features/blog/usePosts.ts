import { useEffect, useState } from "react";
import { blogApi } from "./blogApi";
import { getFallbackPosts } from "./blogFallback";
import type { PostFilterParams, PostListResult } from "./types";

type UsePostsState = PostListResult & {
  loading: boolean;
  error: unknown;
};

type FallbackDecisionInput = {
  postsLength: number;
  params: PostFilterParams;
};

export function shouldUseFallbackPosts({ postsLength, params }: FallbackDecisionInput): boolean {
  if (postsLength > 0) return false;
  return !params.category && !params.tag && !params.year && !params.month && !params.keyword;
}

export function usePosts(params: PostFilterParams = {}): UsePostsState {
  const { category, tag, year, month, keyword, page, pageSize } = params;
  const [state, setState] = useState<UsePostsState>(() => ({
    ...getFallbackPosts({ category, tag, year, month, keyword, page, pageSize }),
    loading: true,
    error: null,
  }));

  useEffect(() => {
    const requestParams: PostFilterParams = { category, tag, year, month, keyword, page, pageSize };
    let active = true;

    async function load() {
      setState((current) => ({ ...current, loading: true, error: null }));
      try {
        const result = keyword ? await blogApi.searchPosts(requestParams) : await blogApi.listPosts(requestParams);

        if (!active) return;
        if (!shouldUseFallbackPosts({ postsLength: result.posts.length, params: requestParams })) {
          setState({ ...result, loading: false, error: null });
          return;
        }

        setState({ ...getFallbackPosts(requestParams), loading: false, error: null });
      } catch (error) {
        if (!active) return;
        setState({ ...getFallbackPosts(requestParams), loading: false, error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [category, tag, year, month, keyword, page, pageSize]);

  return state;
}
