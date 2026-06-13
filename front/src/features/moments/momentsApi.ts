import { apiClient } from "../../lib/apiClient";
import { normalizeMediaUrl } from "../../lib/mediaUrl";
import type { MomentView, MusicLinkView } from "./types";

type RawRecord = Record<string, unknown>;

type PageResponse = {
  list?: unknown;
  total?: unknown;
  page?: unknown;
  page_size?: unknown;
  pageSize?: unknown;
};

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map(stringValue).map((item) => item.trim()).filter(Boolean);
}

function tagsFrom(value: unknown): string[] {
  if (Array.isArray(value)) return stringArray(value);
  return stringValue(value).split(/[，,\s]+/).map((item) => item.trim()).filter(Boolean);
}

function linkFrom(value: unknown): MomentView["link"] {
  if (!isRecord(value)) return null;
  const url = stringValue(value.url).trim();
  if (!url) return null;
  const favicon = stringValue(value.favicon).trim();
  return {
    url,
    title: stringValue(value.title).trim() || url,
    ...(favicon ? { favicon } : {}),
  };
}

function musicFrom(value: unknown): MusicLinkView | null {
  if (!isRecord(value)) return null;
  // Direct URL format: {url, title, artist?, cover?}
  const directUrl = stringValue(value.url).trim();
  if (directUrl) {
    const cover = stringValue(value.cover).trim();
    return {
      url: directUrl,
      title: stringValue(value.title).trim() || directUrl,
      artist: stringValue(value.artist).trim() || undefined,
      ...(cover ? { cover: normalizeMediaUrl(cover) } : {}),
    };
  }
  // Admin format: {server, type, id}
  const server = stringValue(value.server).trim();
  const type = stringValue(value.type).trim();
  const songId = stringValue(value.id).trim();
  if (!songId) return null;
  const musicUrl = buildMusicUrl(server, type, songId);
  if (!musicUrl) return null;
  const typeLabel = type === "playlist" ? "歌单" : type === "album" ? "专辑" : "歌曲";
  const serverLabel = server === "netease" ? "网易云" : server;
  return {
    url: musicUrl,
    title: `${serverLabel} · ${typeLabel}`,
    artist: `ID: ${songId}`,
  };
}

function buildMusicUrl(server: string, type: string, id: string): string {
  if (server === "netease") {
    const path = type === "playlist" ? "playlist" : type === "album" ? "album" : "song";
    return `https://music.163.com/#/${path}?id=${id}`;
  }
  return "";
}

/** Extract media URL from either a plain string or an object like {url, platform, ...} */
function extractMediaUrl(value: unknown): string | undefined {
  if (isRecord(value)) {
    const url = stringValue(value.url).trim();
    return url || undefined;
  }
  const str = stringValue(value).trim();
  return str || undefined;
}

function contentOf(record: RawRecord): RawRecord {
  return isRecord(record.content) ? record.content : record;
}

export function mapMoment(value: unknown): MomentView | null {
  if (!isRecord(value)) return null;
  const content = contentOf(value);
  const text = stringValue(content.text ?? content.content ?? content.body).trim();
  if (!text) return null;
  const tags = tagsFrom(content.tags);
  return {
    id: stringValue(value.id ?? content.id),
    text,
    images: stringArray(content.images).map(normalizeMediaUrl),
    date: stringValue(value.publish_time ?? value.created_at ?? content.date),
    mood: stringValue(content.mood).trim() || tags[0] || "平静",
    tags,
    location: stringValue(content.location).trim(),
    link: linkFrom(content.link),
    video: extractMediaUrl(content.video),
    audio: extractMediaUrl(content.audio),
    music: musicFrom(content.music),
  };
}

export type MomentListResult = {
  moments: MomentView[];
  total: number;
  page: number;
  pageSize: number;
};

export async function fetchMoments(page = 1, pageSize = 30): Promise<MomentListResult> {
  const data = await apiClient.get<PageResponse>("/moments", { page, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  const moments = list.map(mapMoment).filter((moment): moment is MomentView => Boolean(moment));
  return {
    moments,
    total: Number(data.total) || moments.length,
    page: Number(data.page) || page,
    pageSize: Number(data.page_size ?? data.pageSize) || pageSize,
  };
}
