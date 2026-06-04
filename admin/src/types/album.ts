export interface AlbumPhoto {
  id: number;
  album_id: number;
  file_id?: number | null;
  image_url: string;
  title?: string | null;
  description?: string | null;
  sort_order: number;
  is_public: boolean;
  taken_at?: string | null;
  created_at: string;
  updated_at: string;
}

export interface Album {
  id: number;
  title: string;
  slug: string;
  description?: string | null;
  cover_url?: string | null;
  sort_order: number;
  is_public: boolean;
  photo_count: number;
  photos?: AlbumPhoto[];
  created_at: string;
  updated_at: string;
}

export interface AlbumListData {
  list: Album[];
  total: number;
  page: number;
  page_size: number;
}

export interface AlbumListQuery {
  page: number;
  page_size: number;
  keyword?: string;
  is_public?: boolean;
}

export interface AlbumPayload {
  title: string;
  slug: string;
  description?: string;
  cover_url?: string;
  sort_order: number;
  is_public: boolean;
}

export interface AlbumPhotoPayload {
  file_id?: number | null;
  image_url: string;
  title?: string;
  description?: string;
  sort_order: number;
  is_public: boolean;
  taken_at?: string | null;
}
