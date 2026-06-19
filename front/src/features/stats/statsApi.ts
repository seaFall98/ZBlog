import { apiClient } from "../../lib/apiClient";
import { stringValue, type RawRecord } from "../../lib/typeGuards";
import type { ArchiveYear } from "../blog/archive";
import type { SiteStatsView } from "./types";

function numberValue(value: unknown): number {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

export async function fetchSiteStats(): Promise<SiteStatsView> {
  const data = await apiClient.get<RawRecord>("/stats/site");
  return {
    totalArticles: numberValue(data.total_articles),
    totalWords: numberValue(data.total_words),
    totalVisitors: numberValue(data.total_visitors),
    totalVisits: numberValue(data.total_page_views),
    onlineUsers: numberValue(data.online_users),
    totalComments: numberValue(data.total_comments),
    totalFriends: numberValue(data.total_friends),
    totalPhotos: numberValue(data.total_photos),
    totalMessages: numberValue(data.total_guestbook_messages ?? data.total_comments),
    totalMoments: numberValue(data.total_moments),
    totalCategories: numberValue(data.total_categories),
    totalTags: numberValue(data.total_tags),
    todayVisitors: numberValue(data.today_visitors),
    todayPageviews: numberValue(data.today_pageviews),
    yesterdayVisitors: numberValue(data.yesterday_visitors),
    yesterdayPageviews: numberValue(data.yesterday_pageviews),
    monthPageviews: numberValue(data.month_pageviews),
  };
}

type ArchiveStatsResponse = {
  archives?: unknown;
};

export async function fetchArchiveStats(): Promise<ArchiveYear[]> {
  const data = await apiClient.get<ArchiveStatsResponse>("/stats/archives");
  const rows = Array.isArray(data.archives) ? data.archives : [];
  const grouped = new Map<number, Map<number, number>>();

  rows.forEach((row) => {
    if (!row || typeof row !== "object" || Array.isArray(row)) return;
    const record = row as RawRecord;
    const year = Number(record.year);
    const month = Number(record.month);
    const count = numberValue(record.count);
    if (!Number.isInteger(year) || !Number.isInteger(month) || month < 1 || month > 12) return;
    const months = grouped.get(year) ?? new Map<number, number>();
    months.set(month, count);
    grouped.set(year, months);
  });

  return Array.from(grouped.entries())
    .sort(([leftYear], [rightYear]) => rightYear - leftYear)
    .map(([year, months]) => {
      const archiveMonths = Array.from(months.entries())
        .sort(([leftMonth], [rightMonth]) => rightMonth - leftMonth)
        .map(([month, count]) => {
          const paddedMonth = String(month).padStart(2, "0");
          return {
            year,
            month,
            label: `${year} 年 ${paddedMonth} 月`,
            slug: `${year}/${paddedMonth}`,
            posts: [],
            count,
          };
        });

      return {
        year,
        count: archiveMonths.reduce((total, month) => total + month.count, 0),
        months: archiveMonths,
      };
    })
    .filter((group) => stringValue(group.year));
}
