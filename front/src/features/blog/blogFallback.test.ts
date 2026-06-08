import { describe, expect, it } from "vitest";
import { fallbackPosts, findFallbackPostBySlug, mergeFallbackAndApiPosts } from "./blogFallback";
import type { PostListResult, PostView } from "./types";

function makeApiPost(overrides: Partial<PostView> = {}): PostView {
  return {
    id: "api-post",
    slug: "api-post",
    title: "后台文章",
    summary: "后台新增内容",
    contentHtml: "<p>后台新增内容</p>",
    category: null,
    tags: [],
    publishedAt: "2026-06-08T01:01:20.128850Z",
    coverUrl: "",
    readTime: 1,
    featured: false,
    source: "api",
    ...overrides,
  };
}

function makeApiResult(posts: PostView[], overrides: Partial<PostListResult> = {}): PostListResult {
  return {
    posts,
    total: posts.length,
    page: 1,
    pageSize: 50,
    source: "api",
    ...overrides,
  };
}

describe("blogFallback", () => {
  it("finds the Paico autumn-light article by slug", () => {
    const post = findFallbackPostBySlug("autumn-light");

    expect(post).toBeDefined();
    expect(post?.title).toContain("秋日");
    expect(post?.contentHtml).toContain("光");
  });

  it("exposes Paico fallback posts", () => {
    expect(fallbackPosts.length).toBeGreaterThanOrEqual(8);
    expect(fallbackPosts.map((post) => post.slug)).toContain("on-solitude");
  });

  it("keeps Paico seed posts when API returns additional posts and orders newest first", () => {
    const merged = mergeFallbackAndApiPosts(makeApiResult([makeApiPost({ slug: "测试123", title: "测试123" })]), { pageSize: 50 });

    expect(merged.source).toBe("api");
    expect(merged.posts.map((post) => post.slug)).toContain("autumn-light");
    expect(merged.posts.map((post) => post.slug)).toContain("测试123");
    expect(merged.posts[0].slug).toBe("测试123");
  });

  it("lets API posts replace Paico seed posts with the same slug", () => {
    const merged = mergeFallbackAndApiPosts(
      makeApiResult([makeApiPost({ id: "api-autumn", slug: "autumn-light", title: "后端秋日" })]),
      { pageSize: 50 },
    );

    const post = merged.posts.find((item) => item.slug === "autumn-light");
    expect(post?.id).toBe("api-autumn");
    expect(post?.title).toBe("后端秋日");
    expect(merged.posts.filter((item) => item.slug === "autumn-light")).toHaveLength(1);
  });

  it("keeps matching Paico seed posts for filtered API results", () => {
    const merged = mergeFallbackAndApiPosts(makeApiResult([]), { tag: "孤独", pageSize: 50 });

    expect(merged.posts.length).toBeGreaterThan(0);
    expect(merged.posts.every((post) => post.tags.some((tag) => tag.slug === "孤独" || tag.name === "孤独"))).toBe(true);
  });

  it("keeps later API pages unchanged", () => {
    const apiPost = makeApiPost({ slug: "second-page-post" });
    const apiResult = makeApiResult([apiPost], { total: 300, page: 2, pageSize: 1 });

    const merged = mergeFallbackAndApiPosts(apiResult, { page: 2, pageSize: 1 });

    expect(merged).toBe(apiResult);
    expect(merged.posts).toEqual([apiPost]);
  });

  it("keeps all first-page API posts without exceeding page size", () => {
    const apiPosts = [makeApiPost({ slug: "api-one" }), makeApiPost({ slug: "api-two" })];

    const merged = mergeFallbackAndApiPosts(makeApiResult(apiPosts, { total: 300, pageSize: 2 }), { pageSize: 2 });

    expect(merged.posts.map((post) => post.slug)).toEqual(expect.arrayContaining(["api-one", "api-two"]));
    expect(merged.posts).toHaveLength(2);
  });

  it("preserves backend pagination metadata for API-backed first-page results", () => {
    const merged = mergeFallbackAndApiPosts(makeApiResult([makeApiPost()], { total: 300, page: 1, pageSize: 1 }), {
      pageSize: 1,
    });

    expect(merged.total).toBe(300);
    expect(merged.page).toBe(1);
    expect(merged.pageSize).toBe(1);
  });
});
