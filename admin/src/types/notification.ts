import type { PaginationQuery } from './request';

export type NotificationType =
  | 'comment_new'
  | 'comment_reply'
  | 'feedback_new'
  | 'feedback_update'
  | 'article_published'
  | 'system_alert'
  | 'friend_apply'
  | 'friend_abnormal';

export interface CommentNotificationData {
  article_title?: string;
  article_slug?: string;
  comment_id?: number;
  comment_content?: string;
  parent_comment_id?: number;
}

export interface FeedbackNotificationData {
  ticket_no?: string;
  report_url?: string;
  report_type?: string;
  form_content?: Record<string, unknown>;
  status?: string;
}

export interface SystemAlertNotificationData {
  alert_type?: string;
  message?: string;
  severity?: string;
}

export interface FriendApplyNotificationData {
  site_name?: string;
  site_url?: string;
  description?: string;
}

export interface Notification {
  id: number;
  type: NotificationType;
  type_text: string;
  title: string;
  content: string;
  link: string;
  data:
    | CommentNotificationData
    | FeedbackNotificationData
    | SystemAlertNotificationData
    | FriendApplyNotificationData
    | Record<string, unknown>;
  target_id?: number;
  recipient_user_id?: number | null;
  target_type?: string | null;
  target_key?: string | null;
  target_comment_id?: number | null;
  is_read: boolean;
  read_at: string | null;
  is_processed: boolean;
  processed_at: string | null;
  created_at: string;
  sender: string | null;
}

export interface NotificationListData {
  list: Notification[];
  total: number;
  page: number;
  page_size: number;
  unread_count: number;
}

export interface NotificationQueryParams extends PaginationQuery {
  type?: NotificationType | string;
  read?: boolean;
  processed?: boolean;
  keyword?: string;
}
