import request from '@/utils/request';
import type {
  ScheduledJob,
  ScheduledJobHandler,
  ScheduledJobListData,
  ScheduledJobLog,
  ScheduledJobLogListData,
  ScheduledJobUpdatePayload,
} from '@/types/scheduledJob';

export function getScheduledJobs(params: { page: number; page_size: number }): Promise<ScheduledJobListData> {
  return request.get('/admin/scheduled-jobs', { params });
}

// Reserved for future "create job" UI. Endpoint is fully functional and tested.
export function getScheduledJobHandlers(): Promise<ScheduledJobHandler[]> {
  return request.get('/admin/scheduled-jobs/handlers');
}

export function updateScheduledJob(id: number, payload: ScheduledJobUpdatePayload): Promise<ScheduledJob> {
  return request.put(`/admin/scheduled-jobs/${id}`, payload);
}

export function setScheduledJobEnabled(id: number, enabled: boolean): Promise<ScheduledJob> {
  return request.put(`/admin/scheduled-jobs/${id}/enabled`, { enabled });
}

export function runScheduledJob(id: number): Promise<ScheduledJobLog> {
  return request.post(`/admin/scheduled-jobs/${id}/run`);
}

export function getScheduledJobLogs(
  id: number,
  params: { page: number; page_size: number }
): Promise<ScheduledJobLogListData> {
  return request.get(`/admin/scheduled-jobs/${id}/logs`, { params });
}
