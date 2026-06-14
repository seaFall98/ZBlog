export type NotificationItem = {
  id: string;
  type: string;
  typeText: string;
  title: string;
  content: string;
  link: string;
  data: Record<string, unknown>;
  targetId: string;
  recipientUserId: string;
  targetType: string;
  targetKey: string;
  targetCommentId: string;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
  sender: string;
};

export type NotificationPage = {
  list: NotificationItem[];
  total: number;
  page: number;
  pageSize: number;
  unreadCount: number;
};
