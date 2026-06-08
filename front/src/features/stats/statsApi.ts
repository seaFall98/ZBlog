import { apiClient } from "../../lib/apiClient";
import type { SiteStatsView } from "./types";

type RawRecord = Record<string, unknown>;

function numberValue(value: unknown): number {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

export async function fetchSiteStats(): Promise<SiteStatsView> {
  const data = await apiClient.get<RawRecord>("/stats/site");
  return {
    totalArticles: numberValue(data.total_articles),
    totalVisits: numberValue(data.total_page_views),
    totalPhotos: numberValue(data.total_photos),
    totalMessages: numberValue(data.total_guestbook_messages ?? data.total_comments),
    totalMoments: numberValue(data.total_moments),
  };
}
