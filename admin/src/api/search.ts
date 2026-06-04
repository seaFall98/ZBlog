import request from '@/utils/request';

export interface SearchStatus {
  strategy: string;
  elasticsearch_enabled: boolean;
  fallback_to_db: boolean;
  last_reindex?: string | null;
  last_reindex_indexed?: number;
  last_reindex_deleted?: number;
  last_reindex_failed?: number;
  last_error?: string | null;
  last_error_at?: string | null;
}

export interface SearchReindexResult {
  strategy: string;
  indexed: number;
  deleted: number;
  failed: number;
}

export function getSearchStatus(): Promise<SearchStatus> {
  return request.get('/admin/search/status');
}

export function reindexSearch(): Promise<SearchReindexResult> {
  return request.post('/admin/search/reindex');
}
