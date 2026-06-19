import { apiClient } from "../../lib/apiClient";
import type { PageResponse } from "../../lib/apiEnvelope";
import type { FeedbackMessagePayload, FeedbackPage, FeedbackSubmitPayload, FeedbackTicket } from "./types";

function normalizePage(data: PageResponse<FeedbackTicket>): FeedbackPage {
  return {
    list: data.list ?? [],
    total: Number(data.total ?? 0),
    page: Number(data.page ?? 1),
    page_size: Number(data.page_size ?? data.pageSize ?? 10),
  };
}

export const feedbackApi = {
  submit(payload: FeedbackSubmitPayload) {
    return apiClient.post<FeedbackTicket>("/feedback", payload);
  },

  mine(params: { page?: number; pageSize?: number; status?: string } = {}) {
    return apiClient
      .get<PageResponse<FeedbackTicket>>("/feedback/mine", {
        page: params.page ?? 1,
        page_size: params.pageSize ?? 20,
        status: params.status,
      })
      .then(normalizePage);
  },

  byToken(accessToken: string) {
    return apiClient.get<FeedbackTicket>(`/feedback/token/${encodeURIComponent(accessToken)}`);
  },

  addMessage(id: number, payload: FeedbackMessagePayload) {
    return apiClient.post<FeedbackTicket>(`/feedback/${id}/messages`, payload);
  },

  uploadAttachment(file: File) {
    const body = new FormData();
    body.append("file", file);
    body.append("type", "反馈投诉");
    return apiClient.upload<{ file_url: string; original_name: string; file_size: number }>("/upload", body);
  },
};
