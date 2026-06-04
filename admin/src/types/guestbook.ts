export type GuestbookStatus = 'pending' | 'approved' | 'rejected' | 'hidden';

export interface GuestbookMessage {
  id: number;
  nickname: string;
  email?: string | null;
  content: string;
  status: GuestbookStatus;
  pinned: boolean;
  ip?: string | null;
  user_agent?: string | null;
  admin_note?: string | null;
  created_at: string;
  updated_at: string;
}

export interface GuestbookListData {
  list: GuestbookMessage[];
  total: number;
  page: number;
  page_size: number;
}

export interface GuestbookListQuery {
  page: number;
  page_size: number;
  keyword?: string;
  status?: GuestbookStatus;
  pinned?: boolean;
  start_time?: string;
  end_time?: string;
}
