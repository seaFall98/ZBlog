import type { FeedbackTicket } from "./types";

const STORAGE_KEY = "zblog:v2:feedback-history";
const MAX_ITEMS = 30;
const RETENTION_MS = 30 * 24 * 60 * 60 * 1000;

export type StoredFeedbackAccess = {
  ticketNo: string;
  accessToken: string;
  createdAt: number;
  summary: string;
  status?: string;
};

function now() {
  return Date.now();
}

function prune(items: StoredFeedbackAccess[]) {
  const cutoff = now() - RETENTION_MS;
  return items
    .filter((item) => item.accessToken && item.createdAt >= cutoff)
    .sort((left, right) => right.createdAt - left.createdAt)
    .slice(0, MAX_ITEMS);
}

export function readFeedbackHistory(): StoredFeedbackAccess[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    return prune(JSON.parse(raw) as StoredFeedbackAccess[]);
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return [];
  }
}

export function writeFeedbackHistory(items: StoredFeedbackAccess[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(prune(items)));
}

export function rememberFeedback(ticket: FeedbackTicket) {
  if (!ticket.access_token) return;
  const current = readFeedbackHistory().filter((item) => item.accessToken !== ticket.access_token);
  writeFeedbackHistory([
    {
      ticketNo: ticket.ticket_no,
      accessToken: ticket.access_token,
      createdAt: now(),
      summary: ticket.form_content?.description || ticket.report_url || ticket.ticket_no,
      status: ticket.status,
    },
    ...current,
  ]);
}

export function forgetFeedback(accessToken: string) {
  writeFeedbackHistory(readFeedbackHistory().filter((item) => item.accessToken !== accessToken));
}
