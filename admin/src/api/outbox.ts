import request from '@/utils/request';

export interface OutboxEvent {
  id: number;
  event_type: string;
  aggregate_type: string;
  aggregate_id: string;
  payload: string;
  status: string;
  attempts: number;
  error_message?: string | null;
  sent_at?: string | null;
  created_at: string;
  updated_at: string;
}

export interface OutboxListQuery {
  page: number;
  page_size: number;
  status?: string;
}

export interface OutboxListData {
  list: OutboxEvent[];
  total: number;
  page: number;
  page_size: number;
}

export interface OutboxDrainResult {
  published: number;
  failed: number;
  total: number;
}

export function getOutboxEvents(params: OutboxListQuery): Promise<OutboxListData> {
  return request.get('/admin/outbox', { params });
}

export function publishPendingOutboxEvents(): Promise<OutboxDrainResult> {
  return request.post('/admin/outbox/publish-pending');
}
