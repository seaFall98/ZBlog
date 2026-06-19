import { apiClient } from "../../lib/apiClient";

export type CollectPayload = {
  type: "pageview" | "duration" | "event";
  url: string;
  hostname: string;
  title: string;
  referrer: string;
  language: string;
  screen: string;
  article_id?: number;
  event_name?: string;
  event_data?: Record<string, unknown>;
  duration?: number;
  timestamp: number;
};

export type CollectResponse = {
  accepted: boolean;
  visitor_id: string;
  article_view_counted?: boolean;
  article_view_count?: number;
};

export function collect(payload: CollectPayload) {
  return apiClient.post<CollectResponse>("/collect", payload);
}
