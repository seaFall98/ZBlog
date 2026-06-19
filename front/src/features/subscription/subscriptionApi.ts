import { apiClient } from "../../lib/apiClient";

export type SubscribeResponse = {
  id?: number;
  email?: string;
  status?: string;
};

export const subscriptionApi = {
  subscribe(email: string) {
    return apiClient.post<SubscribeResponse>("/subscribe", { email });
  },
};

