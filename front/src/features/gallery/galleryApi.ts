import { apiClient } from "../../lib/apiClient";
import { mapAlbum, mapAlbumList } from "./galleryMapper";
import type { AlbumListResult, AlbumView } from "./types";

type AlbumListParams = {
  page?: number;
  pageSize?: number;
};

export async function fetchAlbums(params: AlbumListParams = {}): Promise<AlbumListResult> {
  const data = await apiClient.get<unknown>("/albums", {
    page: params.page,
    page_size: params.pageSize,
  });
  return mapAlbumList(data, Number(params.page) || 1, Number(params.pageSize) || 20);
}

export async function fetchAlbum(slug: string): Promise<AlbumView | null> {
  const data = await apiClient.get<unknown>(`/albums/${encodeURIComponent(slug)}`);
  return mapAlbum(data);
}
