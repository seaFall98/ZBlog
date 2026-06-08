import { useEffect, useState } from "react";
import { fetchAlbum } from "./galleryApi";
import type { AlbumView } from "./types";

type UseAlbumState = {
  album: AlbumView | null;
  loading: boolean;
  error: unknown;
};

function emptyAlbum(): UseAlbumState {
  return { album: null, loading: false, error: null };
}

export function useAlbum(slug: string): UseAlbumState {
  const [state, setState] = useState<UseAlbumState>(() => ({ ...emptyAlbum(), loading: true }));

  useEffect(() => {
    let active = true;

    async function load() {
      if (!slug) {
        setState(emptyAlbum());
        return;
      }

      setState((current) => ({ ...current, loading: true, error: null }));
      try {
        const album = await fetchAlbum(slug);
        if (!active) return;
        setState({ album, loading: false, error: null });
      } catch (error) {
        if (!active) return;
        setState({ ...emptyAlbum(), error });
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [slug]);

  return state;
}
