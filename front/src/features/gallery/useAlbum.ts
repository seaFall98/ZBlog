import { useQuery } from "@tanstack/react-query";
import { fetchAlbum } from "./galleryApi";
import type { AlbumView } from "./types";

type UseAlbumState = {
  album: AlbumView | null;
  loading: boolean;
  error: unknown;
};

export function useAlbum(slug: string): UseAlbumState {
  const { data, isLoading, error } = useQuery({
    queryKey: ["album", slug],
    queryFn: () => fetchAlbum(slug),
    enabled: !!slug,
  });

  return {
    album: data ?? null,
    loading: isLoading,
    error,
  };
}
