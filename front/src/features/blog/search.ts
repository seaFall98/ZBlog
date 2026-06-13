import type { PostView } from "./types";

export function normalizeQuery(query: string): string {
  return query.trim();
}

type SearchablePostFields = PostView & {
  excerpt?: unknown;
  summary?: unknown;
  tags?: unknown;
};

type SearchableTagFields = {
  name?: unknown;
  slug?: unknown;
};

function textField(value: unknown): string {
  return typeof value === "string" ? value : "";
}

function tagSearchFields(tags: unknown): string[] {
  if (!Array.isArray(tags)) return [];

  return tags.flatMap((tag) => {
    if (typeof tag === "string") return [tag];
    if (!tag || typeof tag !== "object") return [];

    const searchableTag = tag as SearchableTagFields;
    return [textField(searchableTag.name), textField(searchableTag.slug)];
  });
}

/** @deprecated Not used in production — only referenced in tests. */
export function filterPostsByQuery(posts: PostView[], query: string): PostView[] {
  const normalizedQuery = normalizeQuery(query).toLowerCase();
  if (!normalizedQuery) return [];

  return posts.filter((post) => {
    const searchablePost = post as SearchablePostFields;
    const searchableText = [
      post.title,
      textField(searchablePost.excerpt) || textField(searchablePost.summary),
      post.category?.name ?? "",
      post.category?.slug ?? "",
      ...tagSearchFields(searchablePost.tags),
    ]
      .join(" ")
      .toLowerCase();

    return searchableText.includes(normalizedQuery);
  });
}

export function queryFromSearchParams(params: URLSearchParams): string {
  return normalizeQuery(params.get("q") ?? "");
}
