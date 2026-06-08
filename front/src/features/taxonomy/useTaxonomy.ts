import { useEffect, useState } from "react";
import { fetchCategories, fetchTags } from "./taxonomyApi";
import type { TaxonomyItem, TaxonomyResult } from "./types";

type TaxonomyKind = "categories" | "tags";

const taxonomyLoaders: Record<TaxonomyKind, () => Promise<TaxonomyItem[]>> = {
  categories: fetchCategories,
  tags: fetchTags,
};

function useTaxonomy(kind: TaxonomyKind): TaxonomyResult {
  const [result, setResult] = useState<TaxonomyResult>(() => ({
    items: [],
    source: "api",
    loading: true,
    error: null,
  }));

  useEffect(() => {
    let cancelled = false;
    const load = taxonomyLoaders[kind];

    async function loadTaxonomy(): Promise<void> {
      setResult({ items: [], source: "api", loading: true, error: null });
      try {
        const items = await load();
        if (cancelled) return;
        setResult({ items, source: "api", loading: false, error: null });
      } catch (error) {
        if (!cancelled) {
          setResult({ items: [], source: "api", loading: false, error: error as Error });
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
