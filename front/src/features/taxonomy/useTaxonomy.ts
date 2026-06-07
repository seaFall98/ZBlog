import { useEffect, useState } from "react";
import { fetchCategories, fetchTags } from "./taxonomyApi";
import { fallbackCategories, fallbackTags } from "./taxonomyFallback";
import type { TaxonomyItem, TaxonomyResult } from "./types";

type TaxonomyKind = "categories" | "tags";

const taxonomyLoaders: Record<TaxonomyKind, () => Promise<TaxonomyItem[]>> = {
  categories: fetchCategories,
  tags: fetchTags,
};

const taxonomyFallbacks: Record<TaxonomyKind, () => TaxonomyItem[]> = {
  categories: fallbackCategories,
  tags: fallbackTags,
};

function useTaxonomy(kind: TaxonomyKind): TaxonomyResult {
  const [result, setResult] = useState<TaxonomyResult>(() => ({
    items: taxonomyFallbacks[kind](),
    source: "fallback",
    loading: true,
    error: null,
  }));

  useEffect(() => {
    let cancelled = false;
    const load = taxonomyLoaders[kind];
    const fallback = taxonomyFallbacks[kind];

    async function loadTaxonomy(): Promise<void> {
      try {
        const items = await load();
        if (cancelled) return;

        if (items.length > 0) {
          setResult({ items, source: "api", loading: false, error: null });
          return;
        }

        setResult({ items: fallback(), source: "fallback", loading: false, error: null });
      } catch (error) {
        if (!cancelled) {
          setResult({ items: fallback(), source: "fallback", loading: false, error: error as Error });
        }
      }
    }

    void loadTaxonomy();

    return () => {
      cancelled = true;
    };
  }, [kind]);

  return result;
}

export function useCategories(): TaxonomyResult {
  return useTaxonomy("categories");
}

export function useTags(): TaxonomyResult {
  return useTaxonomy("tags");
}
