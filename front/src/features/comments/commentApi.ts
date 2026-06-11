import { apiClient } from "../../lib/apiClient";
import type { CommentSubmitPayload, CommentView } from "./types";

type RawRecord = Record<string, unknown>;
type PageResponse = { list?: unknown };

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function avatarFor(nickname: string): string {
  return `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(nickname || "guest")}`;
}

export function mapComment(value: unknown): CommentView | null {
  if (!isRecord(value)) return null;
  const content = stringValue(value.content).trim();
  if (!content) return null;
  const user = isRecord(value.user) ? value.user : {};
  const nickname = stringValue(value.nickname ?? value.author_name ?? value.name ?? user.nickname).trim() || "访客";
  const replies = Array.isArray(value.replies) ? value.replies.map(mapComment).filter((item): item is CommentView => Boolean(item)) : [];

  return {
    id: stringValue(value.id),
    parentId: stringValue(value.parent_id ?? value.parentId),
    nickname,
    email: stringValue(value.email ?? user.email),
    website: stringValue(value.website ?? user.website),
    content,
    createdAt: stringValue(value.created_at ?? value.createdAt ?? value.date),
    avatar: stringValue(value.avatar ?? user.avatar) || avatarFor(nickname),
    replies,
  };
}

export async function fetchComments(targetType: string, targetKey: string, pageSize = 50): Promise<CommentView[]> {
  const data = await apiClient.get<PageResponse>("/comments", { target_type: targetType, target_key: targetKey, page: 1, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  return list.map(mapComment).filter((comment): comment is CommentView => Boolean(comment));
}

export async function submitComment(payload: CommentSubmitPayload): Promise<CommentView> {
  const data = await apiClient.post<RawRecord>("/comments", payload);
  const comment = mapComment(data);
  if (!comment) throw new Error("评论提交后返回数据无效");
  return comment;
}
