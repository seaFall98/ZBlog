import request from '@/utils/request';

export interface MailOutboxItem {
  id: number;
  audience: string;
  mail_type: string;
  recipient: string;
  subject: string;
  status: string;
  error_message?: string | null;
  attempts: number;
  last_attempt_at?: string | null;
  next_attempt_at?: string | null;
  created_at: string;
  sent_at?: string | null;
  updated_at: string;
}

export interface MailOutboxListData {
  list: MailOutboxItem[];
  total: number;
  page: number;
  page_size: number;
}

export interface MailOutboxListQuery {
  page: number;
  page_size: number;
  status?: string;
}

export interface MailDrainResult {
  total: number;
  sent: number;
  failed: number;
}

export function getMailOutbox(params: MailOutboxListQuery): Promise<MailOutboxListData> {
  return request.get('/admin/mail-outbox', { params });
}

export function drainMailOutbox(limit = 20): Promise<MailDrainResult> {
  return request.post('/admin/mail-outbox/drain', null, { params: { limit } });
}
