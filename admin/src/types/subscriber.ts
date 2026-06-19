import type { PaginationQuery } from './request';

export type SubscriberStatus = 'PENDING' | 'ACTIVE' | 'UNSUBSCRIBED' | 'BOUNCED';

export interface Subscriber {
  id: number;
  email: string;
  active: boolean;
  status: SubscriberStatus;
  status_label?: string;
  status_tone?: string;
  confirmation_token?: string;
  confirmed_at?: string | null;
  unsubscribed_at?: string | null;
  bounced_at?: string | null;
  failure_count?: number;
  last_delivery_status?: string | null;
  last_delivery_error?: string | null;
  last_delivery_at?: string | null;
  created_at: string;
  updated_at: string;
}

export interface SubscriberQuery extends PaginationQuery {
  keyword?: string;
  status?: SubscriberStatus;
}

export interface SubscriberListData {
  list: Subscriber[];
  total: number;
  page: number;
  page_size: number;
}
