import { useQuery } from "@tanstack/react-query";
import { fetchSiteStats } from "./statsApi";
import type { SiteStatsView } from "./types";

export const emptySiteStats: SiteStatsView = {
  totalArticles: 0,
  totalVisits: 0,
  totalPhotos: 0,
  totalMessages: 0,
  totalMoments: 0,
};

type UseSiteStatsState = SiteStatsView & {
  loading: boolean;
  error: unknown;
};

export function useSiteStats(): UseSiteStatsState {
  const { data, isLoading, error } = useQuery({
    queryKey: ["siteStats"],
    queryFn: fetchSiteStats,
  });

  return {
    ...(data ?? emptySiteStats),
    loading: isLoading,
    error,
  };
}
