export type CommentView = {
  id: string;
  parentId: string;
  nickname: string;
  email: string;
  website: string;
  content: string;
  createdAt: string;
  avatar: string;
  replies: CommentView[];
};

export type CommentSubmitPayload = {
  target_type: string;
  target_key: string;
  content: string;
  nickname: string;
  email?: string;
  website?: string;
  parent_id?: number;
};
