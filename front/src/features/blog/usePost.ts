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

function samePost(candidate: PostView, post: PostView): boolean {
  return candidate.id === post.id || candidate.slug === post.slug;
}

type RelatedPostsApi = Pick<typeof blogApi, "listPosts">;

export async function loadRelatedPosts(post: PostView, api: RelatedPostsApi = blogApi): Promise<PostView[]> {
  const related: PostView[] = [];
  const seenIds = new Set<string>();
  const seenSlugs = new Set<string>();
  const tags = post.tags.filter((tag) => tag.slug || tag.name);

  for (const tag of tags) {
    if (related.length >= 3) break;

    const result = await api.listPosts({ page: 1, pageSize: 6, tag: tag.slug || tag.name });
    for (const candidate of result.posts) {
      if (samePost(candidate, post)) continue;
      if ((candidate.id && seenIds.has(candidate.id)) || (candidate.slug && seenSlugs.has(candidate.slug))) continue;

      related.push(candidate);
      if (candidate.id) seenIds.add(candidate.id);
      if (candidate.slug) seenSlugs.add(candidate.slug);
      if (related.length >= 3) break;
    }
  }

  return related;
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
        let related: PostView[] = [];
        if (post.tags.length > 0) {
          try {
            related = await loadRelatedPosts(post);
          } catch (relatedError) {
            console.error("相关文章加载失败:", relatedError);
          }
        }
        if (!active) return;

        setState({
          post,
          related,
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
