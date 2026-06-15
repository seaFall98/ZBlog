export interface ScheduledJob {
  id: number;
  name: string;
  handler_name: string;
  cron_expression: string;
  parameters: Record<string, unknown>;
  enabled: boolean;
  description?: string;
  last_run_at?: string | null;
  created_at: string;
  updated_at: string;
}

export interface ScheduledJobLog {
  id: number;
  job_id: number;
  job_name: string;
  handler_name: string;
  status: 'running' | 'success' | 'failed' | string;
  message?: string | null;
  started_at: string;
  finished_at?: string | null;
  duration_ms?: number | null;
}

export interface ScheduledJobHandler {
  name: string;
  description: string;
  default_parameters: Record<string, unknown>;
}

export interface ScheduledJobListData {
  list: ScheduledJob[];
  total: number;
  page: number;
  page_size: number;
}

export interface ScheduledJobLogListData {
  list: ScheduledJobLog[];
  total: number;
  page: number;
  page_size: number;
}

export interface ScheduledJobUpdatePayload {
  cron_expression: string;
  parameters: Record<string, unknown>;
  enabled: boolean;
}
