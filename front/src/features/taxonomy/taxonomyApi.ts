import { apiClient } from "../../lib/apiClient";
import { mapTaxonomyItems } from "./taxonomyMapper";
import type { TaxonomyItem } from "./types";

export async function fetchCategories(): Promise<TaxonomyItem[]> {
  const data = await apiClient.get<unknown>("/categories");
  return mapTaxonomyItems(data);
}

export async function fetchTags(): Promise<TaxonomyItem[]> {
  const data = await apiClient.get<unknown>("/tags");
  return mapTaxonomyItems(data);
}
