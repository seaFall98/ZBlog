export type SystemMetric = number | 'unsupported';

export interface SystemStatic {
  cpu_core: number;
  cpu_model: string;
  cpu_arch: string;
  hostname: string;
  os: string;
  server_ip: string;
  timezone: string;
  db_type: string;
  memory_total: number;
  swap_total: SystemMetric;
  disk_total: number;
  db_tables: number;
  storage_status: string;
  email_status: string;
  feishu_status: string;
  app_version: string;
}

export interface SystemDynamic {
  cpu_usage: SystemMetric;
  cpu_usage_status?: string;
  load_1: SystemMetric;
  load_5: SystemMetric;
  load_15: SystemMetric;
  memory_used: number;
  memory_available: number;
  swap_used: SystemMetric;
  host_uptime: number;
  disk_used: number;
  disk_free: number;
  db_status: string;
  db_size: SystemMetric;
  db_conn_count: SystemMetric;
  version_latest_version: string;
  version_last_check_error: string;
}
