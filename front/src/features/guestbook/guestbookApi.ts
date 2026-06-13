import { apiClient } from "../../lib/apiClient";
import type { PageResponse } from "../../lib/apiEnvelope";
import { isRecord, stringValue, type RawRecord } from "../../lib/typeGuards";
import type { GuestbookMessageView, GuestbookSubmitResult } from "./types";

type SubmitGuestbookMessage = {
  nickname: string;
  content: string;
};

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

export type GuestbookMessageListResult = {
  messages: GuestbookMessageView[];
  total: number;
  page: number;
  pageSize: number;
};

export async function fetchGuestbookMessages(page = 1, pageSize = 50): Promise<GuestbookMessageListResult> {
  const data = await apiClient.get<PageResponse<unknown>>("/guestbook/messages", { page, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  const messages = list.map(mapGuestbookMessage).filter((message): message is GuestbookMessageView => Boolean(message));
  return {
    messages,
    total: Number(data.total) || messages.length,
    page: Number(data.page) || page,
    pageSize: Number(data.page_size ?? data.pageSize) || pageSize,
  };
}

export async function submitGuestbookMessage(message: SubmitGuestbookMessage): Promise<GuestbookSubmitResult> {
  const data = await apiClient.post<RawRecord>("/guestbook/messages", message);
  return mapGuestbookSubmitResult(data);
}
