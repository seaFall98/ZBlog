import { describe, expect, it } from "vitest";
import { findTaxonomyItemByRouteParam, mapTaxonomyItem, mapTaxonomyItems } from "./taxonomyMapper";

describe("mapTaxonomyItem", () => {
  it("maps category-like record", () => {
    expect(mapTaxonomyItem({ id: 1, name: "生活", slug: "life", article_count: 3 })).toEqual({ id: "1", name: "生活", slug: "life", count: 3 });
  });

  it("uses name as slug when slug is absent", () => {
    expect(mapTaxonomyItem({ name: "阅读", count: 2 })).toEqual({ id: "阅读", name: "阅读", slug: "阅读", count: 2 });
  });

  it("extracts category slug from url instead of using numeric id", () => {
    expect(mapTaxonomyItem({ id: 3, name: "生活", url: "/category/life" })).toEqual({ id: "life", name: "生活", slug: "life", count: 0 });
  });

  it("keeps tag name and extracts tag slug from url", () => {
    expect(mapTaxonomyItem({ id: 1, name: "秋日", url: "/tag/autumn", article_count: 2 })).toEqual({ id: "autumn", name: "秋日", slug: "autumn", count: 2 });
  });

  it("maps category cover URL", () => {
    expect(mapTaxonomyItem({ id: 1, name: "写作", slug: "writing", cover_url: "https://example.com/writing.jpg" })).toMatchObject({
      coverUrl: "https://example.com/writing.jpg",
    });
  });
});

describe("mapTaxonomyItems", () => {
  it("maps taxonomy items from paginated response list", () => {
    const items = mapTaxonomyItems({ list: [{ id: 3, name: "生活", url: "/category/life", article_count: 4 }], total: 1, page: 1, page_size: 20 });

    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({ slug: "life", count: 4 });
  });
});

describe("findTaxonomyItemByRouteParam", () => {
  const items = [
    { id: "1", name: "咖啡", slug: "coffee", count: 5 },
    { id: "2", name: "生活记录", slug: "life", count: 2 },
  ];

  it("matches route slug while preserving the original Chinese tag name for display", () => {
    expect(findTaxonomyItemByRouteParam(items, "coffee")?.name).toBe("咖啡");
  });

  it("can also match an encoded Chinese route name fallback", () => {
    expect(findTaxonomyItemByRouteParam(items, "%E7%94%9F%E6%B4%BB%E8%AE%B0%E5%BD%95")?.slug).toBe("life");
  });
});
