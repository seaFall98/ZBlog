export interface GuestbookMessage {
  id: number;
  nickname: string;
  content: string;
  pinned: boolean;
  created_at: string;
}

export interface GuestbookSubmitPayload {
  nickname: string;
  email?: string;
  content: string;
}

export interface GuestbookSubmitResult {
  id: number;
  status: 'pending' | 'approved';
  message: string;
}
