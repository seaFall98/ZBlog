import { useQuery } from "@tanstack/react-query";
import { fetchCategories, fetchTags } from "./taxonomyApi";
import type { TaxonomyItem, TaxonomyResult } from "./types";

type TaxonomyKind = "categories" | "tags";

const taxonomyLoaders: Record<TaxonomyKind, () => Promise<TaxonomyItem[]>> = {
  categories: fetchCategories,
  tags: fetchTags,
};

function useTaxonomy(kind: TaxonomyKind): TaxonomyResult {
  const { data, isLoading, error } = useQuery({
    queryKey: ["taxonomy", kind],
    queryFn: taxonomyLoaders[kind],
  });

  return {
    items: data ?? [],
    source: "api",
    loading: isLoading,
    error,
  };
}

export function useCategories(): TaxonomyResult {
  return useTaxonomy("categories");
}

export function useTags(): TaxonomyResult {
  return useTaxonomy("tags");
}
