import { apiClient } from "../../lib/apiClient";
import { normalizeMediaUrl } from "../../lib/mediaUrl";
import type { PageResponse } from "../../lib/apiEnvelope";
import { isRecord, stringValue, type RawRecord } from "../../lib/typeGuards";
import type { MomentView, MusicLinkView, VideoSourceView } from "./types";

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
  // Has resolved audio URL — inline player possible
  const audioUrl = stringValue(value.url).trim();
  // Admin format with server/type/id for fallback link
  const server = stringValue(value.server).trim();
  const songId = stringValue(value.id).trim();

  if (!audioUrl && !songId) return null;

  const cover = stringValue(value.cover ?? value.pic).trim();
  const title = stringValue(value.title).trim();
  const artist = stringValue(value.artist).trim();

  if (audioUrl) {
    return {
      url: audioUrl,
      title: title || `${server || "未知"} 音乐`,
      artist: artist || undefined,
      ...(cover ? { cover: normalizeMediaUrl(cover) } : {}),
      server: server || undefined,
      type: stringValue(value.type).trim() || undefined,
      id: songId || undefined,
    };
  }

  // Fallback: only server/type/id, build external link
  const type = stringValue(value.type).trim();
  const typeLabel = type === "playlist" ? "歌单" : type === "album" ? "专辑" : "歌曲";
  const serverLabel = server === "netease" ? "网易云" : server;
  return {
    url: buildMusicUrl(server, type, songId),
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

/** Extract video source including platform info for iframe embedding */
function extractVideoSource(value: unknown): VideoSourceView | undefined {
  if (isRecord(value)) {
    const url = stringValue(value.url).trim();
    if (!url) return undefined;
    return {
      url,
      platform: stringValue(value.platform).trim() || undefined,
      videoId: stringValue(value.video_id).trim() || undefined,
    };
  }
  const str = stringValue(value).trim();
  return str || undefined ? { url: str } : undefined;
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
    video: extractVideoSource(content.video),
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
  const data = await apiClient.get<PageResponse<unknown>>("/moments", { page, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  const moments = list.map(mapMoment).filter((moment): moment is MomentView => Boolean(moment));
  return {
    moments,
    total: Number(data.total) || moments.length,
    page: Number(data.page) || page,
    pageSize: Number(data.page_size ?? data.pageSize) || pageSize,
  };
}
