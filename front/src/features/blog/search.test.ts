import { describe, expect, it } from "vitest";
import { filterPostsByQuery, normalizeQuery, queryFromSearchParams } from "./search";
import type { PostView } from "./types";

function makePost(overrides: Partial<PostView> & Pick<PostView, "id" | "title">): PostView {
  return {
    id: overrides.id,
    slug: overrides.id,
    title: overrides.title,
    summary: "一段摘要",
    contentHtml: "<p>正文</p>",
    contentMarkdown: "",
    toc: [],
    category: { id: "life", slug: "life", name: "生活" },
    tags: [],
    publishedAt: "2026-06-01",
    coverUrl: "",
    readTime: 3,
    featured: false,
    source: "api",
    ...overrides,
  };
}

describe("normalizeQuery", () => {
  it("trims whitespace and returns an empty string for blank queries", () => {
    expect(normalizeQuery("  秋日  ")).toBe("秋日");
    expect(normalizeQuery(" \t\n ")).toBe("");
  });
});

describe("filterPostsByQuery", () => {
  it("matches Chinese keywords in title, excerpt, and category", () => {
    const posts = [
      makePost({ id: "title", title: "秋日散步" }),
      makePost({ id: "excerpt", title: "普通标题", summary: "不应命中", excerpt: "记录城市黄昏" } as Partial<PostView> & Pick<PostView, "id" | "title">),
      makePost({ id: "category", title: "分类文章", category: { id: "travel", slug: "travel", name: "旅行" } }),
      makePost({ id: "miss", title: "阅读笔记" }),
    ];

    expect(filterPostsByQuery(posts, "秋日").map((post) => post.id)).toEqual(["title"]);
    expect(filterPostsByQuery(posts, "黄昏").map((post) => post.id)).toEqual(["excerpt"]);
    expect(filterPostsByQuery(posts, "旅行").map((post) => post.id)).toEqual(["category"]);
  });

  it("matches tag name and slug case-insensitively", () => {
    const posts = [
      makePost({
        id: "tag-name",
        title: "标签名",
        tags: [{ id: "autumn", slug: "autumn", name: "秋日" }],
      }),
      makePost({
        id: "tag-slug",
        title: "标签 slug",
        tags: [{ id: "city-walk", slug: "City-Walk", name: "城市漫步" }],
      }),
    ];

    expect(filterPostsByQuery(posts, "秋日").map((post) => post.id)).toEqual(["tag-name"]);
    expect(filterPostsByQuery(posts, "city-walk").map((post) => post.id)).toEqual(["tag-slug"]);
  });

  it("matches string tag fallback values", () => {
    const posts = [
      makePost({ id: "string-tag", title: "字符串标签", tags: ["秋日"] as unknown as PostView["tags"] }),
      makePost({ id: "miss", title: "普通文章" }),
    ];

    expect(filterPostsByQuery(posts, "秋日").map((post) => post.id)).toEqual(["string-tag"]);
  });

  it("returns an empty list for blank queries", () => {
    expect(filterPostsByQuery([makePost({ id: "post", title: "秋日" })], "   ")).toEqual([]);
  });
});

describe("queryFromSearchParams", () => {
  it("reads q from URLSearchParams and normalizes it", () => {
    expect(queryFromSearchParams(new URLSearchParams("q=%20%E7%A7%8B%E6%97%A5%20"))).toBe("秋日");
    expect(queryFromSearchParams(new URLSearchParams("keyword=%E7%A7%8B%E6%97%A5"))).toBe("");
  });
});
