import { describe, expect, it } from "vitest";
import { selectFeaturedPosts, selectGalleryPreviewPosts, selectRecentPosts } from "./homeSelectors";
import type { PostView } from "./types";

type PostViewLike = PostView & {
  coverImage?: string;
  date?: string;
};

function makePost(overrides: Partial<PostViewLike> & Pick<PostView, "id" | "publishedAt">): PostViewLike {
  return {
    id: overrides.id,
    slug: overrides.id,
    title: overrides.id,
    summary: "一段摘要",
    contentHtml: "<p>正文</p>",
    contentMarkdown: "",
    toc: [],
    category: { id: "life", slug: "life", name: "生活" },
    tags: [],
    publishedAt: overrides.publishedAt,
    coverUrl: "",
    readTime: 3,
    featured: false,
    source: "api",
    ...overrides,
  };
}

function makeDateOnlyPost(overrides: Partial<PostViewLike> & Pick<PostViewLike, "id" | "date">): PostViewLike {
  return {
    id: overrides.id,
    slug: overrides.id,
    title: overrides.id,
    summary: "一段摘要",
    contentHtml: "<p>正文</p>",
    contentMarkdown: "",
    toc: [],
    category: { id: "life", slug: "life", name: "生活" },
    tags: [],
    coverUrl: "",
    readTime: 3,
    featured: false,
    source: "api",
    ...overrides,
  } as PostViewLike;
}

describe("selectFeaturedPosts", () => {
  it("uses featured posts first and fills with latest posts without duplicates", () => {
    const posts = [
      makePost({ id: "older-featured", publishedAt: "2026-05-01", featured: true }),
      makePost({ id: "latest", publishedAt: "2026-06-10" }),
      makePost({ id: "newer-featured", publishedAt: "2026-06-01", featured: true }),
      makePost({ id: "middle", publishedAt: "2026-05-20" }),
    ];

    expect(selectFeaturedPosts(posts, 3).map((post) => post.id)).toEqual(["newer-featured", "older-featured", "latest"]);
  });

  it("fills featured posts by descending date without duplicates", () => {
    const posts = [
      makeDateOnlyPost({ id: "date-only-featured", date: "2026-06-01", featured: true }),
      makeDateOnlyPost({ id: "date-only-latest-fill", date: "2026-06-10" }),
      makePost({ id: "published-fill", publishedAt: "2026-05-01" }),
    ];

    expect(selectFeaturedPosts(posts, 3).map((post) => post.id)).toEqual(["date-only-featured", "date-only-latest-fill", "published-fill"]);
  });
});

describe("selectRecentPosts", () => {
  it("returns posts by descending date and does not mutate input", () => {
    const posts = [
      makePost({ id: "old", publishedAt: "2025-12-31" }),
      makePost({ id: "new", publishedAt: "2026-06-08T10:00:00Z" }),
      makePost({ id: "middle", publishedAt: "2026-01-01" }),
    ];
    const originalOrder = posts.map((post) => post.id);

    expect(selectRecentPosts(posts, 2).map((post) => post.id)).toEqual(["new", "middle"]);
    expect(posts.map((post) => post.id)).toEqual(originalOrder);
  });

  it("sorts by date when a post-like object only provides date", () => {
    const posts = [
      makePost({ id: "published-newer", publishedAt: "2026-06-01" }),
      makeDateOnlyPost({ id: "date-only-latest", date: "2026-06-10T10:00:00Z" }),
      makePost({ id: "date-overrides-published", publishedAt: "2026-01-01", date: "2026-06-05" }),
      makePost({ id: "published-older", publishedAt: "2026-05-01" }),
    ];

    expect(selectRecentPosts(posts, 4).map((post) => post.id)).toEqual([
      "date-only-latest",
      "date-overrides-published",
      "published-newer",
      "published-older",
    ]);
  });

  it("treats impossible calendar dates as invalid instead of normalizing them ahead of valid dates", () => {
    const posts = [
      makeDateOnlyPost({ id: "invalid-calendar", date: "2026-06-31" }),
      makePost({ id: "valid", publishedAt: "2026-06-30" }),
    ];

    expect(selectRecentPosts(posts, 2).map((post) => post.id)).toEqual(["valid", "invalid-calendar"]);
  });

  it("falls back to publishedAt when date is an impossible calendar date", () => {
    const posts = [
      makePost({ id: "invalid-date-with-publishedAt", date: "2026-06-31", publishedAt: "2026-06-01" }),
      makePost({ id: "valid-newer", publishedAt: "2026-06-30" }),
      makeDateOnlyPost({ id: "undated", date: "" }),
    ];

    expect(selectRecentPosts(posts, 3).map((post) => post.id)).toEqual(["valid-newer", "invalid-date-with-publishedAt", "undated"]);
  });
});

describe("selectGalleryPreviewPosts", () => {
  it("returns only posts with cover images by descending date and does not mutate input", () => {
    const posts = [
      makePost({ id: "covered-old", publishedAt: "2026-05-01", coverUrl: "", coverImage: "/old.jpg" }),
      makePost({ id: "no-cover", publishedAt: "2026-06-10", coverUrl: "", coverImage: "" }),
      makePost({ id: "covered-new", publishedAt: "2026-06-01", coverUrl: "", coverImage: "/new.jpg" }),
    ];
    const originalOrder = posts.map((post) => post.id);

    expect(selectGalleryPreviewPosts(posts, 6).map((post) => post.id)).toEqual(["covered-new", "covered-old"]);
    expect(posts.map((post) => post.id)).toEqual(originalOrder);
  });

  it("sorts coverImage posts by descending date", () => {
    const posts = [
      makeDateOnlyPost({ id: "covered-date-old", date: "2026-05-01", coverImage: "/old.jpg" }),
      makeDateOnlyPost({ id: "covered-date-new", date: "2026-06-01", coverImage: "/new.jpg" }),
      makeDateOnlyPost({ id: "date-new-without-cover", date: "2026-06-10", coverImage: "" }),
    ];

    expect(selectGalleryPreviewPosts(posts, 6).map((post) => post.id)).toEqual(["covered-date-new", "covered-date-old"]);
  });
});
