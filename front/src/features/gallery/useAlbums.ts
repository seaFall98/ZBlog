import { useEffect, useState } from "react";
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
  const [state, setState] = useState<UseAlbumsState>(() => ({ ...emptyAlbums(1, pageSize), loading: true, error: null }));

  useEffect(() => {
    let active = true;

    async function load() {
      setState({ ...emptyAlbums(1, pageSize), loading: true, error: null });
      try {
        const result = await fetchAlbums({ page: 1, pageSize });
        if (!active) return;
        setState({ ...result, loading: false, error: null });
      } catch (error) {
        if (!active) return;
        setState({ ...emptyAlbums(1, pageSize), loading: false, error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [pageSize]);

  return state;
}
