import { apiClient } from "../../lib/apiClient";
import type { GuestbookMessageView } from "./types";

type RawRecord = Record<string, unknown>;
type PageResponse = { list?: unknown };

type SubmitGuestbookMessage = {
  nickname: string;
  content: string;
};

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function mapMessage(value: unknown): GuestbookMessageView | null {
  if (!isRecord(value)) return null;
  const name = stringValue(value.nickname ?? value.name);
  const content = stringValue(value.content);
  if (!content) return null;
  return {
    id: stringValue(value.id),
    name: name || "访客",
    content,
    date: stringValue(value.created_at ?? value.date),
    avatar: `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(name || "guest")}`,
  };
}

export async function fetchGuestbookMessages(pageSize = 50): Promise<GuestbookMessageView[]> {
  const data = await apiClient.get<PageResponse>("/guestbook/messages", { page: 1, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  return list.map(mapMessage).filter((message): message is GuestbookMessageView => Boolean(message));
}

export async function submitGuestbookMessage(message: SubmitGuestbookMessage): Promise<RawRecord> {
  return apiClient.post<RawRecord>("/guestbook/messages", message);
}
