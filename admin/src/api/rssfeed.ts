import request from '@/utils/request';
import type { RssArticleQuery, RssArticleListData } from '@/types/rssfeed';

/**
 * 全部标记已读响应
 */
export interface MarkAllReadResponse {
  affected: number;
}

export interface RefreshRssFeedsResponse {
  sources: number;
  fetched: number;
  inserted: number;
  failed: number;
  results: Array<{
    friend_id: number;
    status: 'success' | 'failed';
    inserted?: number;
    error?: string;
  }>;
}

/**
 * 获取RSS文章列表
 * @param params 查询参数
 * @returns Promise<RssArticleListData>
 */
export const getRssArticles = async (params?: RssArticleQuery): Promise<RssArticleListData> => {
  return request.get('/admin/rssfeed', { params });
};

/**
 * 手动刷新RSS源
 * @returns Promise<RefreshRssFeedsResponse>
 */
export const refreshRssFeeds = async (): Promise<RefreshRssFeedsResponse> => {
  return request.post('/admin/rssfeed/refresh');
};

/**
 * 标记文章已读
 * @param id 文章ID
 * @returns Promise<void>
 */
export const markRssArticleRead = async (id: number): Promise<void> => {
  await request.put(`/admin/rssfeed/${id}/read`);
};

/**
 * 全部标记已读
 * @returns Promise<MarkAllReadResponse>
 */
export const markAllRssArticlesRead = async (): Promise<MarkAllReadResponse> => {
  return request.put('/admin/rssfeed/read-all');
};
