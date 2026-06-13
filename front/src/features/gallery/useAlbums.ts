import { useQuery } from "@tanstack/react-query";
import { fetchAlbums } from "./galleryApi";
import type { AlbumListResult } from "./types";

type UseAlbumsState = AlbumListResult & {
  loading: boolean;
  error: unknown;
};

function emptyAlbums(page = 1, pageSize = 20): AlbumListResult {
  return { albums: [], total: 0, page, pageSize };
}

export function useAlbums(page = 1, pageSize = 20): UseAlbumsState {
  const { data, isLoading, error } = useQuery({
    queryKey: ["albums", page, pageSize],
    queryFn: () => fetchAlbums({ page, pageSize }),
  });

  return {
    ...(data ?? emptyAlbums(page, pageSize)),
    loading: isLoading,
    error,
  };
}
