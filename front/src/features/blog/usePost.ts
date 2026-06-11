import { useQuery } from "@tanstack/react-query";
import { blogApi } from "./blogApi";
import type { DataSource, PostView } from "./types";

type UsePostState = {
  post: PostView | null;
  related: PostView[];
  loading: boolean;
  error: unknown;
  source: DataSource;
};

function samePost(candidate: PostView, post: PostView): boolean {
  return candidate.id === post.id || candidate.slug === post.slug;
}

export async function loadRelatedPosts(
  post: PostView,
  api: Pick<typeof blogApi, "listPosts"> = blogApi,
): Promise<PostView[]> {
  const tags = post.tags
    .map((tag) => tag.slug || tag.name)
    .filter(Boolean)
    .slice(0, 4);

  if (tags.length === 0) {
    return [];
  }

  const responses = await Promise.allSettled(
    tags.map((tag) => api.listPosts({ page: 1, pageSize: 6, tag })),
  );

  const related: PostView[] = [];
  const seenIds = new Set<string>();
  const seenSlugs = new Set<string>();

  for (const response of responses) {
    if (response.status !== "fulfilled") {
      continue;
    }

    for (const candidate of response.value.posts) {
      if (samePost(candidate, post)) continue;
      if (candidate.id && seenIds.has(candidate.id)) continue;
      if (candidate.slug && seenSlugs.has(candidate.slug)) continue;

      related.push(candidate);
      if (candidate.id) seenIds.add(candidate.id);
      if (candidate.slug) seenSlugs.add(candidate.slug);
      if (related.length >= 3) {
        return related;
      }
    }
  }

  return related;
}

export function usePost(slug: string): UsePostState {
  const {
    data: post,
    isLoading: postLoading,
    error: postError,
  } = useQuery({
    queryKey: ["post", slug],
    queryFn: () => blogApi.getPost(slug),
    enabled: !!slug,
  });

  const { data: related = [] } = useQuery({
    queryKey: ["relatedPosts", slug],
    queryFn: () => loadRelatedPosts(post!),
    enabled: !!post && post.tags.length > 0,
  });

  return {
    post: post ?? null,
    related,
    loading: postLoading,
    error: postError,
    source: "api",
  };
}
