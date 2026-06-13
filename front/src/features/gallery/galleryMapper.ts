import { normalizeMediaUrl } from "../../lib/mediaUrl";
import { isRecord, stringValue as toStringValue, type RawRecord } from "../../lib/typeGuards";
import type { AlbumListResult, AlbumPhotoView, AlbumView } from "./types";

type AlbumListResponse = { list?: unknown; total?: unknown; page?: unknown; page_size?: unknown; pageSize?: unknown } | unknown[];

function firstValue(record: RawRecord, keys: string[]): unknown {
  return keys.map((key) => record[key]).find((value) => value !== undefined && value !== null && value !== "");
}

function toNumberValue(value: unknown): number {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function filenameFromUrl(url: string): string {
  const clean = url.split("?")[0]?.replace(/\/+$/, "") ?? "";
  return decodeURIComponent(clean.split("/").filter(Boolean).pop() ?? "");
}

function mapPhoto(value: unknown): AlbumPhotoView | null {
  if (!isRecord(value)) return null;
  const imageUrl = normalizeMediaUrl(toStringValue(firstValue(value, ["image_url", "imageUrl", "src", "url"])));
  if (!imageUrl) return null;
  const id = toStringValue(firstValue(value, ["id", "image_url", "imageUrl"])) || imageUrl;
  const albumId = toStringValue(firstValue(value, ["album_id", "albumId"]));
  const title = toStringValue(firstValue(value, ["title", "name"])) || filenameFromUrl(imageUrl);
  const takenAt = toStringValue(firstValue(value, ["taken_at", "takenAt", "created_at", "createdAt", "date"]));
  return {
    id,
    albumId,
    imageUrl,
    title,
    description: toStringValue(firstValue(value, ["description", "summary"])),
    takenAt,
    filename: filenameFromUrl(imageUrl),
  };
}

export function mapAlbum(value: unknown): AlbumView | null {
  if (!isRecord(value)) return null;
  const title = toStringValue(firstValue(value, ["title", "name"]));
  const slug = toStringValue(firstValue(value, ["slug", "id"]));
  if (!title || !slug) return null;
  const rawPhotos = firstValue(value, ["photos"]);
  const photos = Array.isArray(rawPhotos) ? rawPhotos.map(mapPhoto).filter((photo): photo is AlbumPhotoView => Boolean(photo)) : [];

  return {
    id: toStringValue(firstValue(value, ["id", "slug"])) || slug,
    slug,
    title,
    description: toStringValue(firstValue(value, ["description", "summary"])),
    coverUrl: normalizeMediaUrl(toStringValue(firstValue(value, ["cover_url", "coverUrl", "coverImage"]))),
    photoCount: toNumberValue(firstValue(value, ["photo_count", "photoCount"])) || photos.length,
    createdAt: toStringValue(firstValue(value, ["created_at", "createdAt", "date"])),
    photos,
  };
}

export function mapAlbumList(data: AlbumListResponse, fallbackPage = 1, fallbackPageSize = 20): AlbumListResult {
  const list = Array.isArray(data) ? data : data?.list;
  const albums = Array.isArray(list) ? list.map(mapAlbum).filter((album): album is AlbumView => Boolean(album)) : [];

  return {
    albums,
    total: Array.isArray(data) ? albums.length : toNumberValue(data?.total) || albums.length,
    page: Array.isArray(data) ? fallbackPage : toNumberValue(data?.page) || fallbackPage,
    pageSize: Array.isArray(data) ? fallbackPageSize : toNumberValue(data?.page_size ?? data?.pageSize) || fallbackPageSize,
  };
}
