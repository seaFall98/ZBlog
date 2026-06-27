export type CommentView = {
  id: string;
  parentId: string;
  rootId: string;
  userId: number;
  nickname: string;
  email: string;
  website: string;
  badge: string;
  role: string;
  replyUser: {
    id: number;
    nickname: string;
    avatar: string;
    badge: string;
    website: string;
    role: string;
  } | null;
  content: string;
  createdAt: string;
  avatar: string;
  isDeleted: boolean;
  likeCount: number;
  likedByMe: boolean;
  pinned: boolean;
  pinnedAt: string;
  replyTotal: number;
  replyPage: number;
  replyPageSize: number;
  replyTotalPages: number;
  replies: CommentView[];
};

export type CommentPage = {
  list: CommentView[];
  total: number;
  page: number;
  pageSize: number;
};

export type CommentLocation = {
  targetType: string;
  targetKey: string;
  commentId: string;
  rootId: string;
  rootPage: number;
  replyPage: number;
  pageSize: number;
  replyPageSize: number;
  isRoot: boolean;
};

export type CommentSubmitPayload = {
  target_type: string;
  target_key: string;
  content: string;
  nickname?: string;
  email?: string;
  website?: string;
  parent_id?: number;
};

export type CommentSort = "hot" | "latest";

export type CommentImageUploadResponse = {
  id: number;
  file_url: string;
  file_name: string;
  original_name: string;
  file_size: number;
};
