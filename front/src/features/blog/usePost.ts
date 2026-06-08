import { useEffect, useMemo, useState } from "react";
import { blogApi } from "./blogApi";
import { findFallbackPostBySlug, getRelatedFallbackPosts } from "./blogFallback";
import type { DataSource, PostView } from "./types";

type UsePostState = {
  post: PostView | null;
  related: PostView[];
  loading: boolean;
  error: unknown;
  source: DataSource;
};

function fallbackState(slug: string): UsePostState {
  const post = findFallbackPostBySlug(slug) ?? null;
  return {
    post,
    related: post ? getRelatedFallbackPosts(post) : [],
    loading: false,
    error: null,
    source: "fallback",
  };
}

export function usePost(slug: string): UsePostState {
  const stableSlug = useMemo(() => slug, [slug]);
  const [state, setState] = useState<UsePostState>(() => ({ ...fallbackState(stableSlug), loading: true }));

  useEffect(() => {
    let active = true;

    async function load() {
      setState((current) => ({ ...current, loading: true, error: null }));
      try {
        const post = await blogApi.getPost(stableSlug);
        if (!active) return;

        if (post) {
          setState({
            post,
            related: getRelatedFallbackPosts(post),
            loading: false,
            error: null,
            source: "api",
          });
          return;
        }

        setState(fallbackState(stableSlug));
      } catch (error) {
        if (!active) return;
        setState({ ...fallbackState(stableSlug), error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [stableSlug]);

  return state;
}
