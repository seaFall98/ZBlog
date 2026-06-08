export type AlbumPhotoView = {
  id: string;
  albumId: string;
  imageUrl: string;
  title: string;
  description: string;
  takenAt: string;
  filename: string;
};

export type AlbumView = {
  id: string;
  slug: string;
  title: string;
  description: string;
  coverUrl: string;
  photoCount: number;
  createdAt: string;
  photos: AlbumPhotoView[];
};

export type AlbumListResult = {
  albums: AlbumView[];
  total: number;
  page: number;
  pageSize: number;
};
