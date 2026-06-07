import { describe, expect, it } from "vitest";
import { buildArchive, filterPostsByArchiveMonth } from "./archive";
import type { PostView } from "./types";

function makePost(overrides: Partial<PostView> & Pick<PostView, "id" | "publishedAt" | "title">): PostView {
  return {
    id: overrides.id,
    slug: overrides.id,
    title: overrides.title,
    summary: "一段摘要",
    contentHtml: "<p>正文</p>",
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

function makeDateOnlyPost(overrides: { id: string; title: string; date: string }): PostView {
  return {
    id: overrides.id,
    slug: overrides.id,
    title: overrides.title,
    summary: "一段摘要",
    contentHtml: "<p>正文</p>",
    category: { id: "life", slug: "life", name: "生活" },
    tags: [],
    date: overrides.date,
    coverUrl: "",
    readTime: 3,
    featured: false,
    source: "api",
  } as unknown as PostView;
}

describe("buildArchive", () => {
  it("groups posts across years and months with descending order and padded labels", () => {
    const posts = [
      makePost({ id: "old-june", title: "旧六月", publishedAt: "2025-06-30" }),
      makePost({ id: "new-may", title: "新五月", publishedAt: "2026-05-01T08:00:00Z" }),
      makePost({ id: "new-june-later", title: "新六月晚", publishedAt: "2026-06-08T09:00:00Z" }),
      makePost({ id: "new-june-earlier", title: "新六月早", publishedAt: "2026-06-01" }),
      makePost({ id: "invalid", title: "无日期", publishedAt: "not-a-date" }),
    ];

    const archive = buildArchive(posts);

    expect(archive.map((year) => year.year)).toEqual([2026, 2025]);
    expect(archive[0]).toMatchObject({ year: 2026, count: 3 });
    expect(archive[0].months.map((month) => month.month)).toEqual([6, 5]);
    expect(archive[0].months[0]).toMatchObject({
      year: 2026,
      month: 6,
      label: "2026 年 06 月",
      slug: "2026/06",
      count: 2,
    });
    expect(archive[0].months[0].posts.map((post) => post.id)).toEqual(["new-june-later", "new-june-earlier"]);
    expect(archive.flatMap((year) => year.months).flatMap((month) => month.posts.map((post) => post.id))).not.toContain("invalid");
  });

  it("groups posts that only provide the planned date field", () => {
    const posts = [
      makeDateOnlyPost({ id: "june-later", title: "六月晚", date: "2026-06-08T09:00:00+08:00" }),
      makeDateOnlyPost({ id: "june-earlier", title: "六月早", date: "2026-06-01" }),
      makeDateOnlyPost({ id: "may", title: "五月", date: "2026-05-31" }),
    ];

    const archive = buildArchive(posts);

    expect(archive).toHaveLength(1);
    expect(archive[0]).toMatchObject({ year: 2026, count: 3 });
    expect(archive[0].months.map((month) => month.slug)).toEqual(["2026/06", "2026/05"]);
    expect(archive[0].months[0].posts.map((post) => post.id)).toEqual(["june-later", "june-earlier"]);
  });

  it("groups UTC instants by the Asia/Shanghai display month", () => {
    const posts = [makePost({ id: "july-display", title: "上海七月展示", publishedAt: "2026-06-30T16:30:00Z" })];

    const archive = buildArchive(posts);

    expect(archive).toHaveLength(1);
    expect(archive[0].months.map((month) => month.slug)).toEqual(["2026/07"]);
    expect(archive[0].months[0].posts.map((post) => post.id)).toEqual(["july-display"]);
  });

  it("ignores impossible calendar dates instead of letting Date normalize them", () => {
    const posts = [
      makeDateOnlyPost({ id: "valid", title: "有效日期", date: "2026-02-28" }),
      makeDateOnlyPost({ id: "invalid", title: "无效日期", date: "2026-02-31" }),
      makePost({ id: "invalid-z", title: "无效 UTC 日期", publishedAt: "2026-02-31T00:00:00Z" }),
    ];

    const archive = buildArchive(posts);

    expect(archive).toHaveLength(1);
    expect(archive[0].months[0]).toMatchObject({ year: 2026, month: 2, count: 1 });
    expect(archive[0].months[0].posts.map((post) => post.id)).toEqual(["valid"]);
  });
});

describe("filterPostsByArchiveMonth", () => {
  it("filters posts by numeric or string year and month while ignoring invalid dates", () => {
    const posts = [
      makePost({ id: "june", title: "六月", publishedAt: "2026-06-02T10:00:00Z" }),
      makePost({ id: "may", title: "五月", publishedAt: "2026-05-31" }),
      makePost({ id: "old", title: "旧文", publishedAt: "2025-06-01" }),
      makePost({ id: "invalid", title: "无日期", publishedAt: "" }),
    ];

    expect(filterPostsByArchiveMonth(posts, "2026", "06").map((post) => post.id)).toEqual(["june"]);
    expect(filterPostsByArchiveMonth(posts, 2026, 6).map((post) => post.id)).toEqual(["june"]);
  });

  it("filters posts that only provide date and ignores invalid calendar dates", () => {
    const posts = [
      makeDateOnlyPost({ id: "june", title: "六月", date: "2026-06-02T10:00:00+08:00" }),
      makeDateOnlyPost({ id: "may", title: "五月", date: "2026-05-31" }),
      makeDateOnlyPost({ id: "normalized", title: "归一化非法日期", date: "2026-06-31" }),
    ];

    expect(filterPostsByArchiveMonth(posts, "2026", "06").map((post) => post.id)).toEqual(["june"]);
  });

  it("filters UTC instants by the Asia/Shanghai display month", () => {
    const posts = [makePost({ id: "july-display", title: "上海七月展示", publishedAt: "2026-06-30T16:30:00Z" })];

    expect(filterPostsByArchiveMonth(posts, 2026, 7).map((post) => post.id)).toEqual(["july-display"]);
    expect(filterPostsByArchiveMonth(posts, 2026, 6).map((post) => post.id)).toEqual([]);
  });
});
