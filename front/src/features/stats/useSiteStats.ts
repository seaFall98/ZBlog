import { useEffect, useState } from "react";
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
  const [state, setState] = useState<UseSiteStatsState>(() => ({ ...emptySiteStats, loading: true, error: null }));

  useEffect(() => {
    let active = true;

    async function load() {
      setState((current) => ({ ...current, loading: true, error: null }));
      try {
        const stats = await fetchSiteStats();
        if (!active) return;
        setState({ ...stats, loading: false, error: null });
      } catch (error) {
        if (!active) return;
        setState({ ...emptySiteStats, loading: false, error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, []);

  return state;
}
