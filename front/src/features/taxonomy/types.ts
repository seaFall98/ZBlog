export type TaxonomyItem = {
  id: string;
  name: string;
  slug: string;
  count: number;
  description?: string;
  coverUrl?: string;
};

export type TaxonomyResult = {
  items: TaxonomyItem[];
  source: "api";
  loading: boolean;
  error: Error | null;
};
