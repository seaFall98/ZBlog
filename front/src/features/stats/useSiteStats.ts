import { useQuery } from "@tanstack/react-query";
import { fetchSiteStats } from "./statsApi";
import type { SiteStatsView } from "./types";

export const emptySiteStats: SiteStatsView = {
  totalArticles: 0,
  totalWords: 0,
  totalVisitors: 0,
  totalVisits: 0,
  onlineUsers: 0,
  totalComments: 0,
  totalFriends: 0,
  totalPhotos: 0,
  totalMessages: 0,
  totalMoments: 0,
  totalCategories: 0,
  totalTags: 0,
  todayVisitors: 0,
  todayPageviews: 0,
  yesterdayVisitors: 0,
  yesterdayPageviews: 0,
  monthPageviews: 0,
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
