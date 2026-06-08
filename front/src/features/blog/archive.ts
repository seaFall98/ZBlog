import { toDateText } from "../../lib/text";
import type { PostView } from "./types";

export type ArchiveMonth = {
  year: number;
  month: number;
  label: string;
  slug: string;
  posts: PostView[];
  count: number;
};

export type ArchiveYear = {
  year: number;
  count: number;
  months: ArchiveMonth[];
};

type ParsedPostDate = {
  year: number;
  month: number;
  time: number;
};

type PostDateFields = PostView & {
  date?: unknown;
  publishedAt?: unknown;
};

function isExactCalendarDate(year: number, month: number, day: number): boolean {
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) return false;
  if (month < 1 || month > 12 || day < 1) return false;

  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
}

function parseDateValue(value: unknown): ParsedPostDate | null {
  if (typeof value !== "string") return null;

  const originalMatch = value.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T\s]|$)/);
  if (!originalMatch) return null;

  const originalYear = Number(originalMatch[1]);
  const originalMonth = Number(originalMatch[2]);
  const originalDay = Number(originalMatch[3]);
  if (!isExactCalendarDate(originalYear, originalMonth, originalDay)) return null;

  const displayDate = toDateText(value);
  const displayMatch = displayDate.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!displayMatch) return null;

  const year = Number(displayMatch[1]);
  const month = Number(displayMatch[2]);
  const day = Number(displayMatch[3]);
  if (!isExactCalendarDate(year, month, day)) return null;

  const time = Date.UTC(year, month - 1, day);
  return { year, month, time };
}

function parsePostDate(post: PostView): ParsedPostDate | null {
  const dateFields = post as PostDateFields;
  return parseDateValue(dateFields.date) ?? parseDateValue(dateFields.publishedAt);
}

function padMonth(month: number): string {
  return String(month).padStart(2, "0");
}

function byPublishedAtDesc(left: PostView, right: PostView): number {
  const leftDate = parsePostDate(left);
  const rightDate = parsePostDate(right);
  return (rightDate?.time ?? 0) - (leftDate?.time ?? 0);
}

export function buildArchive(posts: PostView[]): ArchiveYear[] {
  const grouped = new Map<number, Map<number, PostView[]>>();

  posts.forEach((post) => {
    const parsed = parsePostDate(post);
    if (!parsed) return;

    const months = grouped.get(parsed.year) ?? new Map<number, PostView[]>();
    const monthPosts = months.get(parsed.month) ?? [];
    monthPosts.push(post);
    months.set(parsed.month, monthPosts);
    grouped.set(parsed.year, months);
  });

  return Array.from(grouped.entries())
    .sort(([leftYear], [rightYear]) => rightYear - leftYear)
    .map(([year, months]) => {
      const archiveMonths = Array.from(months.entries())
        .sort(([leftMonth], [rightMonth]) => rightMonth - leftMonth)
        .map(([month, monthPosts]) => {
          const paddedMonth = padMonth(month);
          const sortedPosts = [...monthPosts].sort(byPublishedAtDesc);
          return {
            year,
            month,
            label: `${year} 年 ${paddedMonth} 月`,
            slug: `${year}/${paddedMonth}`,
            posts: sortedPosts,
            count: sortedPosts.length,
          };
        });

      return {
        year,
        count: archiveMonths.reduce((total, month) => total + month.count, 0),
        months: archiveMonths,
      };
    });
}

export function filterPostsByArchiveMonth(posts: PostView[], year: number | string, month: number | string): PostView[] {
  const targetYear = Number(year);
  const targetMonth = Number(month);

  if (!Number.isInteger(targetYear) || !Number.isInteger(targetMonth)) return [];

  return posts.filter((post) => {
    const parsed = parsePostDate(post);
    return parsed?.year === targetYear && parsed.month === targetMonth;
  });
}
