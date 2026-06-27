import { apiClient } from "../../lib/apiClient";
import { isRecord, stringValue, type RawRecord } from "../../lib/typeGuards";
import type {
  CommentImageUploadResponse,
  CommentLocation,
  CommentPage,
  CommentSort,
  CommentSubmitPayload,
  CommentView,
} from "./types";

type PageResponse = { list?: unknown; total?: unknown; page?: unknown; page_size?: unknown; pageSize?: unknown };

function avatarFor(nickname: string): string {
  return `https://api.dicebear.com/7.x/thumbs/svg?seed=${encodeURIComponent(nickname || "guest")}`;
}

function numberValue(value: unknown): number {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
}

function boolValue(value: unknown): boolean {
  return value === true || value === "true" || value === 1 || value === "1";
}

function mapReplyUser(value: unknown): CommentView["replyUser"] {
  if (!isRecord(value)) return null;
  const nickname = stringValue(value.nickname).trim();
  if (!nickname) return null;
  return {
    id: numberValue(value.id),
    nickname,
    avatar: stringValue(value.avatar),
    badge: stringValue(value.badge),
    website: stringValue(value.website),
    role: stringValue(value.role) || "guest",
  };
}

export function mapComment(value: unknown): CommentView | null {
  if (!isRecord(value)) return null;
  const content = stringValue(value.content).trim();
  if (!content) return null;
  const user = isRecord(value.user) ? value.user : {};
  const nickname = stringValue(value.nickname ?? value.author_name ?? value.name ?? user.nickname).trim() || "访客";
  const replies = Array.isArray(value.replies) ? value.replies.map(mapComment).filter((item): item is CommentView => Boolean(item)) : [];
  const isDeleted = Boolean(value.is_deleted ?? value.isDeleted);

  return {
    id: stringValue(value.id),
    parentId: stringValue(value.parent_id ?? value.parentId),
    rootId: stringValue(value.root_id ?? value.rootId ?? value.id),
    userId: numberValue(value.user_id ?? user.id),
    nickname,
    email: stringValue(value.email ?? user.email),
    website: stringValue(value.website ?? user.website),
    badge: stringValue(value.badge ?? user.badge),
    role: stringValue(value.role ?? user.role),
    replyUser: mapReplyUser(value.reply_user ?? value.replyUser),
    content,
    createdAt: stringValue(value.created_at ?? value.createdAt ?? value.date),
    avatar: stringValue(value.avatar ?? user.avatar) || avatarFor(nickname),
    isDeleted,
    likeCount: numberValue(value.like_count ?? value.likeCount),
    likedByMe: boolValue(value.liked_by_me ?? value.likedByMe),
    pinned: boolValue(value.pinned),
    pinnedAt: stringValue(value.pinned_at ?? value.pinnedAt),
    replyTotal: numberValue(value.reply_total ?? value.replyTotal ?? replies.length),
    replyPage: Math.max(1, numberValue(value.reply_page ?? value.replyPage) || 1),
    replyPageSize: Math.max(1, numberValue(value.reply_page_size ?? value.replyPageSize) || 10),
    replyTotalPages: numberValue(value.reply_total_pages ?? value.replyTotalPages),
    replies,
  };
}

export async function fetchCommentPage(
  targetType: string,
  targetKey: string,
  page = 1,
  pageSize = 10,
  replyPageSize = 10,
  sort: CommentSort = "hot",
): Promise<CommentPage> {
  const data = await apiClient.get<PageResponse>("/comments", {
    target_type: targetType,
    target_key: targetKey,
    page,
    page_size: pageSize,
    reply_page_size: replyPageSize,
    sort,
  });
  const list = Array.isArray(data.list) ? data.list : [];
  return {
    list: list.map(mapComment).filter((comment): comment is CommentView => Boolean(comment)),
    total: numberValue(data.total),
    page: numberValue(data.page) || page,
    pageSize: numberValue(data.page_size ?? data.pageSize) || pageSize,
  };
}

export async function fetchComments(targetType: string, targetKey: string, pageSize = 50): Promise<CommentView[]> {
  const page = await fetchCommentPage(targetType, targetKey, 1, pageSize, 10);
  return page.list;
}

export async function fetchReplies(rootId: string | number, page = 1, pageSize = 10): Promise<CommentPage> {
  const data = await apiClient.get<PageResponse>(`/comments/${rootId}/replies`, { page, page_size: pageSize });
  const list = Array.isArray(data.list) ? data.list : [];
  return {
    list: list.map(mapComment).filter((comment): comment is CommentView => Boolean(comment)),
    total: numberValue(data.total),
    page: numberValue(data.page) || page,
    pageSize: numberValue(data.page_size ?? data.pageSize) || pageSize,
  };
}

export async function locateComment(targetType: string, targetKey: string, commentId: string | number, pageSize = 10, replyPageSize = 10): Promise<CommentLocation> {
  const data = await apiClient.get<RawRecord>("/comments/locate", {
    target_type: targetType,
    target_key: targetKey,
    comment_id: commentId,
    page_size: pageSize,
    reply_page_size: replyPageSize,
  });
  return {
    targetType: stringValue(data.target_type ?? data.targetType),
    targetKey: stringValue(data.target_key ?? data.targetKey),
    commentId: stringValue(data.comment_id ?? data.commentId),
    rootId: stringValue(data.root_id ?? data.rootId),
    rootPage: numberValue(data.root_page ?? data.rootPage) || 1,
    replyPage: numberValue(data.reply_page ?? data.replyPage) || 1,
    pageSize: numberValue(data.page_size ?? data.pageSize) || pageSize,
    replyPageSize: numberValue(data.reply_page_size ?? data.replyPageSize) || replyPageSize,
    isRoot: boolValue(data.is_root ?? data.isRoot),
  };
}

export async function submitComment(payload: CommentSubmitPayload): Promise<CommentView> {
  const data = await apiClient.post<RawRecord>("/comments", payload);
  const comment = mapComment(data);
  if (!comment) throw new Error("评论提交后返回数据无效");
  return comment;
}

export async function deleteComment(id: string | number): Promise<void> {
  await apiClient.delete<null>(`/comments/${id}`);
}

export async function toggleCommentLike(id: string | number): Promise<CommentView> {
  const data = await apiClient.post<RawRecord>(`/comments/${id}/like`, {});
  const comment = mapComment(data);
  if (!comment) throw new Error("点赞后返回数据无效");
  return comment;
}

export async function uploadCommentImage(file: File): Promise<CommentImageUploadResponse> {
  const body = new FormData();
  body.append("file", file);
  body.append("type", "评论贴图");
  return apiClient.upload<CommentImageUploadResponse>("/upload", body);
}
