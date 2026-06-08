import { useEffect, useMemo, useState } from "react";
import { blogApi } from "./blogApi";
import type { DataSource, PostView } from "./types";

type UsePostState = {
  post: PostView | null;
  related: PostView[];
  loading: boolean;
  error: unknown;
  source: DataSource;
};

function emptyState(): UsePostState {
  return {
    post: null,
    related: [],
    loading: false,
    error: null,
    source: "api",
  };
}

export function usePost(slug: string): UsePostState {
  const stableSlug = useMemo(() => slug, [slug]);
  const [state, setState] = useState<UsePostState>(() => ({ ...emptyState(), loading: true }));

  useEffect(() => {
    let active = true;

    async function load() {
      if (!stableSlug) {
        setState(emptyState());
        return;
      }

      setState((current) => ({ ...current, loading: true, error: null }));
      try {
        const post = await blogApi.getPost(stableSlug);
        if (!active) return;

        setState({
          post,
          related: [],
          loading: false,
          error: null,
          source: "api",
        });
      } catch (error) {
        if (!active) return;
        setState({ ...emptyState(), error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [stableSlug]);

  return state;
}
