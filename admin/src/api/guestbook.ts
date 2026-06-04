import request from '@/utils/request';
import type { GuestbookListData, GuestbookListQuery, GuestbookMessage, GuestbookStatus } from '@/types/guestbook';

export function getGuestbookMessages(params: GuestbookListQuery): Promise<GuestbookListData> {
  return request.get('/admin/guestbook/messages', { params });
}

export function updateGuestbookStatus(
  id: number,
  status: GuestbookStatus,
  adminNote?: string
): Promise<GuestbookMessage> {
  return request.put(`/admin/guestbook/messages/${id}/status`, {
    status,
    admin_note: adminNote,
  });
}

export function updateGuestbookPin(id: number, pinned: boolean): Promise<GuestbookMessage> {
  return request.put(`/admin/guestbook/messages/${id}/pin`, { pinned });
}

export function deleteGuestbookMessage(id: number): Promise<void> {
  return request.delete(`/admin/guestbook/messages/${id}`);
}
