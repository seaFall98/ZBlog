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

export function useAlbums(pageSize = 20): UseAlbumsState {
  const { data, isLoading, error } = useQuery({
    queryKey: ["albums", pageSize],
    queryFn: () => fetchAlbums({ page: 1, pageSize }),
  });

  return {
    ...(data ?? emptyAlbums(1, pageSize)),
    loading: isLoading,
    error,
  };
}
