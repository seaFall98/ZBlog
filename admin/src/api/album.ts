import request from '@/utils/request';
import type { Album, AlbumListData, AlbumListQuery, AlbumPayload, AlbumPhoto, AlbumPhotoPayload } from '@/types/album';

export function getAlbums(params: AlbumListQuery): Promise<AlbumListData> {
  return request.get('/admin/albums', { params });
}

export function getAlbum(id: number): Promise<Album> {
  return request.get(`/admin/albums/${id}`);
}

export function createAlbum(data: AlbumPayload): Promise<Album> {
  return request.post('/admin/albums', data);
}

export function updateAlbum(id: number, data: AlbumPayload): Promise<Album> {
  return request.put(`/admin/albums/${id}`, data);
}

export function deleteAlbum(id: number): Promise<void> {
  return request.delete(`/admin/albums/${id}`);
}

export function addAlbumPhoto(albumId: number, data: AlbumPhotoPayload): Promise<AlbumPhoto> {
  return request.post(`/admin/albums/${albumId}/photos`, data);
}

export function updateAlbumPhoto(albumId: number, photoId: number, data: AlbumPhotoPayload): Promise<AlbumPhoto> {
  return request.put(`/admin/albums/${albumId}/photos/${photoId}`, data);
}

export function deleteAlbumPhoto(albumId: number, photoId: number): Promise<void> {
  return request.delete(`/admin/albums/${albumId}/photos/${photoId}`);
}

export function reorderAlbumPhotos(albumId: number, photoIds: number[]): Promise<AlbumPhoto[]> {
  return request.put(`/admin/albums/${albumId}/photos/reorder`, { photo_ids: photoIds });
}
