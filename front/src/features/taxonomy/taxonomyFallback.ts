import { fallbackPosts } from "../blog/blogFallback";
import type { TaxonomyItem } from "./types";

function addTaxonomyItem(items: Map<string, TaxonomyItem>, item: Omit<TaxonomyItem, "count">): void {
  const key = item.slug || item.name || item.id;
  const existing = items.get(key);
  if (existing) {
    items.set(key, { ...existing, count: existing.count + 1 });
    return;
  }

  items.set(key, { ...item, count: 1 });
}

export function fallbackCategories(): TaxonomyItem[] {
  const categories = new Map<string, TaxonomyItem>();

  fallbackPosts.forEach((post) => {
    if (!post.category) return;
    const slug = post.category.slug || post.category.name;
    const id = post.category.id || slug;
    addTaxonomyItem(categories, { id, name: post.category.name, slug });
  });

  return [...categories.values()];
}

export function fallbackTags(): TaxonomyItem[] {
  const tags = new Map<string, TaxonomyItem>();

  fallbackPosts.forEach((post) => {
    post.tags.forEach((tag) => {
      const slug = tag.slug || tag.name;
      const id = tag.id || slug;
      addTaxonomyItem(tags, { id, name: tag.name, slug });
    });
  });

  return [...tags.values()];
}

export function mergeFallbackAndApiTaxonomy(fallbackItems: TaxonomyItem[], apiItems: TaxonomyItem[]): TaxonomyItem[] {
  const items = new Map<string, TaxonomyItem>();

  fallbackItems.forEach((item) => {
    const key = item.slug || item.name || item.id;
    items.set(key, item);
  });

  apiItems.forEach((item) => {
    const key = item.slug || item.name || item.id;
    items.set(key, item);
  });

  return [...items.values()];
}
