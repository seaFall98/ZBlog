import type { GuestbookMessage, GuestbookSubmitPayload, GuestbookSubmitResult } from '@@/types/guestbook';
import type { PaginationQuery } from '@@/types/request';
import { createApi } from './createApi';

const guestbookApi = createApi<GuestbookMessage>('/guestbook/messages');

export const getGuestbookMessages = async (params: PaginationQuery = {}) => {
  return guestbookApi.getList(params);
};

export const submitGuestbookMessage = async (payload: GuestbookSubmitPayload) => {
  return guestbookApi.post<GuestbookSubmitResult>('', payload);
};
