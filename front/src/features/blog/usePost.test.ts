import { describe, expect, it, vi } from "vitest";
import { loadRelatedPosts } from "./usePost";
import type { PostView } from "./types";

function postWith(overrides: Partial<PostView>): PostView {
  return {
    id: "current-id",
    slug: "current-slug",
    title: "当前文章",
    summary: "",
    contentHtml: "",
    contentMarkdown: "",
    toc: [],
    category: null,
    tags: [],
    publishedAt: "",
    coverUrl: "",
    readTime: 1,
    featured: false,
    source: "api",
    ...overrides,
  };
}

describe("loadRelatedPosts", () => {
  it("loads real list API results by current article tags, excludes current article, deduplicates, and caps at 3", async () => {
    const current = postWith({
      id: "1",
      slug: "stream-reduce",
      tags: [
        { id: "java", slug: "java", name: "Java" },
        { id: "stream", slug: "stream", name: "Stream" },
      ],
    });
    const relatedA = postWith({ id: "2", slug: "java-a", title: "Java A" });
    const relatedB = postWith({ id: "3", slug: "java-b", title: "Java B" });
    const relatedC = postWith({ id: "4", slug: "stream-c", title: "Stream C" });
    const relatedD = postWith({ id: "5", slug: "stream-d", title: "Stream D" });
    const listPosts = vi
      .fn()
      .mockResolvedValueOnce({ posts: [current, relatedA, relatedB, relatedA], total: 4, page: 1, pageSize: 6, source: "api" })
      .mockResolvedValueOnce({ posts: [relatedB, relatedC, relatedD], total: 3, page: 1, pageSize: 6, source: "api" });

    const related = await loadRelatedPosts(current, { listPosts });

    expect(listPosts).toHaveBeenNthCalledWith(1, { page: 1, pageSize: 6, tag: "java" });
    expect(listPosts).toHaveBeenNthCalledWith(2, { page: 1, pageSize: 6, tag: "stream" });
    expect(related.map((post) => post.slug)).toEqual(["java-a", "java-b", "stream-c"]);
  });

  it("uses tag name when slug is absent", async () => {
    const current = postWith({ tags: [{ id: "coffee", slug: "", name: "咖啡" }] });
    const listPosts = vi.fn().mockResolvedValue({ posts: [], total: 0, page: 1, pageSize: 6, source: "api" });

    await loadRelatedPosts(current, { listPosts });

    expect(listPosts).toHaveBeenCalledWith({ page: 1, pageSize: 6, tag: "咖啡" });
  });
});
