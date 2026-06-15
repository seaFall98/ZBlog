import { apiClient } from "../../lib/apiClient";
import type { NotificationItem, NotificationPage } from "./notificationTypes";

type RawNotification = {
  id?: string | number;
  type?: string;
  type_text?: string;
  title?: string;
  content?: string;
  link?: string;
  data?: Record<string, unknown>;
  target_id?: string | number | null;
  recipient_user_id?: string | number | null;
  target_type?: string | null;
  target_key?: string | null;
  target_comment_id?: string | number | null;
  is_read?: boolean;
  read_at?: string | null;
  created_at?: string;
  sender?: string;
};

type RawNotificationPage = {
  list?: RawNotification[];
  total?: number;
  page?: number;
  page_size?: number;
  unread_count?: number;
};

function toStringId(value: string | number | null | undefined) {
  return value === null || value === undefined ? "" : String(value);
}

export function mapNotification(raw: RawNotification): NotificationItem {
  return {
    id: toStringId(raw.id),
    type: raw.type ?? "system",
    typeText: raw.type_text ?? "系统通知",
    title: raw.title ?? "",
    content: raw.content ?? "",
    link: raw.link ?? "",
    data: raw.data ?? {},
    targetId: toStringId(raw.target_id),
    recipientUserId: toStringId(raw.recipient_user_id),
    targetType: raw.target_type ?? "",
    targetKey: raw.target_key ?? "",
    targetCommentId: toStringId(raw.target_comment_id),
    isRead: Boolean(raw.is_read),
    readAt: raw.read_at ?? null,
    createdAt: raw.created_at ?? "",
    sender: raw.sender ?? "",
  };
}

function mapPage(raw: RawNotificationPage): NotificationPage {
  return {
    list: (raw.list ?? []).map(mapNotification),
    total: raw.total ?? 0,
    page: raw.page ?? 1,
    pageSize: raw.page_size ?? 10,
    unreadCount: raw.unread_count ?? 0,
  };
}

export const notificationApi = {
  async list(params: { page?: number; pageSize?: number; unreadOnly?: boolean } = {}) {
    const raw = await apiClient.get<RawNotificationPage>("/notifications", {
      page: params.page ?? 1,
      page_size: params.pageSize ?? 10,
      unread_only: params.unreadOnly ?? false,
    });
    return mapPage(raw);
  },

  async unreadCount() {
    const raw = await apiClient.get<{ unread_count?: number }>("/notifications/unread-count");
    return raw.unread_count ?? 0;
  },

  markRead(id: string) {
    return apiClient.put<NotificationItem>(`/notifications/${id}/read`);
  },

  markAllRead() {
    return apiClient.put<{ affected: number; unread_count: number }>("/notifications/read-all");
  },
};
