import type { PostView } from "./types";

type DatedPostFields = PostView & {
  date?: unknown;
  publishedAt?: unknown;
};

function isExactCalendarDate(year: number, month: number, day: number): boolean {
  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) return false;
  if (month < 1 || month > 12 || day < 1) return false;

  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
}

function parseDateValue(value: unknown): number | null {
  if (typeof value !== "string" || value.trim() === "") return null;

  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T\s]|$)/);
  if (!match) return null;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (!isExactCalendarDate(year, month, day)) return null;

  const time = value.length === 10 ? Date.UTC(year, month - 1, day) : Date.parse(value);
  return Number.isNaN(time) ? null : time;
}

function postTime(post: PostView): number | null {
  const datedPost = post as DatedPostFields;
  return parseDateValue(datedPost.date) ?? parseDateValue(datedPost.publishedAt);
}

function byDateDesc(left: PostView, right: PostView): number {
  const leftTime = postTime(left);
  const rightTime = postTime(right);

  if (leftTime === null && rightTime === null) return 0;
  if (leftTime === null) return 1;
  if (rightTime === null) return -1;

  return rightTime - leftTime;
}

function latestPosts(posts: PostView[]): PostView[] {
  return [...posts].sort(byDateDesc);
}

export function selectFeaturedPosts(posts: PostView[], limit = 3): PostView[] {
  const selected: PostView[] = [];
  const seen = new Set<string>();
  const addPost = (post: PostView) => {
    if (selected.length >= limit || seen.has(post.id)) return;
    selected.push(post);
    seen.add(post.id);
  };

  latestPosts(posts)
    .filter((post) => post.featured)
    .forEach(addPost);
  latestPosts(posts).forEach(addPost);

  return selected;
}

export function selectRecentPosts(posts: PostView[], limit = 5): PostView[] {
  return latestPosts(posts).slice(0, limit);
}

type GalleryPostFields = PostView & {
  coverImage?: unknown;
  coverUrl?: unknown;
};

function hasCoverImage(post: PostView): boolean {
  const galleryPost = post as GalleryPostFields;
  return Boolean(galleryPost.coverImage || galleryPost.coverUrl);
}

export function selectGalleryPreviewPosts(posts: PostView[], limit = 6): PostView[] {
  return latestPosts(posts)
    .filter(hasCoverImage)
    .slice(0, limit);
}
