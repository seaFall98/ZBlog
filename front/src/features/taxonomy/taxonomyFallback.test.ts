import { describe, expect, it } from "vitest";
import { fallbackCategories, fallbackTags, mergeFallbackAndApiTaxonomy } from "./taxonomyFallback";
import type { TaxonomyItem } from "./types";

function item(overrides: Partial<TaxonomyItem>): TaxonomyItem {
  return {
    id: "api",
    name: "后台分类",
    slug: "api",
    count: 1,
    ...overrides,
  };
}

describe("taxonomyFallback", () => {
  it("merges fallback categories with API categories", () => {
    const merged = mergeFallbackAndApiTaxonomy(fallbackCategories(), [item({ id: "default", name: "默认分类", slug: "default", count: 1 })]);

    expect(merged.map((category) => category.slug)).toContain("life");
    expect(merged.map((category) => category.slug)).toContain("default");
  });

  it("merges fallback tags with API tags", () => {
    const merged = mergeFallbackAndApiTaxonomy(fallbackTags(), [item({ id: "zblog", name: "ZBlog", slug: "zblog", count: 1 })]);

    expect(merged.map((tag) => tag.name)).toContain("孤独");
    expect(merged.map((tag) => tag.name)).toContain("ZBlog");
  });

  it("uses API taxonomy item when it has the same slug as fallback", () => {
    const merged = mergeFallbackAndApiTaxonomy([item({ id: "life", name: "生活", slug: "life", count: 2 })], [item({ id: "life-api", name: "生活", slug: "life", count: 3 })]);

    expect(merged).toEqual([{ id: "life-api", name: "生活", slug: "life", count: 3 }]);
  });
});
