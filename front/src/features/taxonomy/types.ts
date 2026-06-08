export type TaxonomyItem = {
  id: string;
  name: string;
  slug: string;
  count: number;
};

export type TaxonomyResult = {
  items: TaxonomyItem[];
  source: "api" | "fallback";
  loading: boolean;
  error: Error | null;
};
