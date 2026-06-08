import { useEffect, useState } from "react";
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

export function usePosts(params: PostFilterParams = {}): UsePostsState {
  const { category, tag, year, month, keyword, page, pageSize } = params;
  const [state, setState] = useState<UsePostsState>(() => ({
    ...emptyPosts({ category, tag, year, month, keyword, page, pageSize }),
    loading: true,
    error: null,
  }));

  useEffect(() => {
    const requestParams: PostFilterParams = { category, tag, year, month, keyword, page, pageSize };
    let active = true;

    async function load() {
      setState({ ...emptyPosts(requestParams), loading: true, error: null });
      try {
        const result = keyword ? await blogApi.searchPosts(requestParams) : await blogApi.listPosts(requestParams);

        if (!active) return;
        setState({ ...result, loading: false, error: null });
      } catch (error) {
        if (!active) return;
        setState({ ...emptyPosts(requestParams), loading: false, error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [category, tag, year, month, keyword, page, pageSize]);

  return state;
}
