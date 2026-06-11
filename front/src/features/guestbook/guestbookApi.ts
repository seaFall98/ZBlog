import { apiClient } from "../../lib/apiClient";
import type { GuestbookMessageView, GuestbookSubmitResult } from "./types";

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

export function mapGuestbookMessage(value: unknown): GuestbookMessageView | null {
  if (!isRecord(value)) return null;
  const name = stringValue(value.nickname ?? value.name).trim();
  const content = stringValue(value.content).trim();
  if (!content) return null;
  return {
    id: stringValue(value.id),
    name: name || "访客",
    content,
    date: stringValue(value.created_at ?? value.date),
    avatar: `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(name || "guest")}`,
  };
}

export function mapGuestbookSubmitResult(value: unknown): GuestbookSubmitResult {
  const record = isRecord(value) ? value : {};
  return {
    id: stringValue(record.id),
    status: stringValue(record.status) || "unknown",
    message: stringValue(record.message) || "留言已提交",
  };
}

export async function fetchGuestbookMessages(pageSize = 50): Promise<GuestbookMessageView[]> {
  const data = await apiClient.get<PageResponse>("/guestbook/messages", { page: 1, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  return list.map(mapGuestbookMessage).filter((message): message is GuestbookMessageView => Boolean(message));
}

export async function submitGuestbookMessage(message: SubmitGuestbookMessage): Promise<GuestbookSubmitResult> {
  const data = await apiClient.post<RawRecord>("/guestbook/messages", message);
  return mapGuestbookSubmitResult(data);
}
