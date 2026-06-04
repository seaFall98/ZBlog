// AI功能请求类型
export interface AISummaryRequest {
  content: string;
}

export interface AIAISummaryRequest {
  content: string;
}

export interface AITitleRequest {
  content: string;
}

// AI功能响应类型
interface AISummaryPolicyMeta {
  summary: string;
  trimmed?: boolean;
  over_limit?: boolean;
  regenerated?: boolean;
  max_length?: number;
  original_length?: number;
  final_length?: number;
}

export interface AISummaryResponse extends AISummaryPolicyMeta {}

export interface AIAISummaryResponse extends AISummaryPolicyMeta {}

export interface AITitleResponse {
  title: string;
}
