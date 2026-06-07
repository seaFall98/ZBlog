import { describe, expect, it } from "vitest";
import { fallbackPosts, findFallbackPostBySlug } from "./blogFallback";
import { shouldUseFallbackPosts } from "./usePosts";

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

  it("uses fallback for successful empty unfiltered API lists", () => {
    expect(shouldUseFallbackPosts({ postsLength: 0, params: {} })).toBe(true);
    expect(shouldUseFallbackPosts({ postsLength: 0, params: { page: 1, pageSize: 10 } })).toBe(true);
  });

  it("keeps successful empty filtered API lists empty", () => {
    expect(shouldUseFallbackPosts({ postsLength: 0, params: { keyword: "秋日" } })).toBe(false);
    expect(shouldUseFallbackPosts({ postsLength: 0, params: { category: "life" } })).toBe(false);
    expect(shouldUseFallbackPosts({ postsLength: 0, params: { tag: "autumn" } })).toBe(false);
    expect(shouldUseFallbackPosts({ postsLength: 0, params: { year: 2026 } })).toBe(false);
    expect(shouldUseFallbackPosts({ postsLength: 0, params: { month: 6 } })).toBe(false);
  });

  it("does not use fallback when API returns posts", () => {
    expect(shouldUseFallbackPosts({ postsLength: 1, params: {} })).toBe(false);
  });
});
