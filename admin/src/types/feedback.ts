export type ReportType = 'copyright' | 'inappropriate' | 'summary' | 'suggestion';

export type FeedbackStatus = 'PENDING' | 'IN_PROGRESS' | 'WAITING_USER' | 'RESOLVED' | 'CLOSED';

export type FeedbackActorType = 'USER' | 'ADMIN' | 'SYSTEM';

export type FeedbackMessageType = 'MESSAGE' | 'STATUS_CHANGE';

export interface FeedbackContent {
  description?: string;
  reason?: string;
  attachmentFiles?: string[];
}

export interface FeedbackMessage {
  id: number;
  feedback_id: number;
  actor_type: FeedbackActorType;
  actor_user_id?: number | null;
  message_type: FeedbackMessageType;
  content: string;
  attachments?: string[];
  from_status?: FeedbackStatus | null;
  to_status?: FeedbackStatus | null;
  created_at: string;
}

export interface Feedback {
  id: number;
  ticket_no: string;
  access_token?: string;
  user_id?: number | null;
  report_url: string;
  report_type: ReportType;
  form_content: FeedbackContent;
  email?: string;
  status: FeedbackStatus;
  status_label?: string;
  status_tone?: string;
  allowed_next_statuses?: FeedbackStatus[];
  admin_reply?: string;
  reply_time?: string | null;
  user_agent?: string;
  ip?: string;
  feedback_time: string;
  updated_at?: string;
  messages?: FeedbackMessage[];
}

export interface FeedbackListQuery {
  page: number;
  page_size: number;
  keyword?: string;
  report_type?: ReportType;
  status?: FeedbackStatus;
  start_time?: string;
  end_time?: string;
}

export interface FeedbackListData {
  list: Feedback[];
  total: number;
  page: number;
  page_size: number;
}

export interface FeedbackUpdateRequest {
  status?: FeedbackStatus;
  admin_reply?: string;
}

export interface FeedbackStatusRequest {
  status: FeedbackStatus;
  content?: string;
}

export interface FeedbackMessageRequest {
  content: string;
  attachmentFiles?: string[];
}
