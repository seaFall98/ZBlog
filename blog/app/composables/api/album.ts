import type { Album } from '@@/types/album';
import type { PaginationQuery } from '@@/types/request';
import { createApi } from './createApi';

const albumApi = createApi<Album>('/albums');

export const getAlbums = async (params: PaginationQuery = {}) => {
  return albumApi.getList(params);
};

export const getAlbumBySlug = async (slug: string) => {
  return albumApi.getOne(slug);
};
