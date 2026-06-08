import { useEffect, useState } from "react";
import { fetchFriendLinks } from "./linksApi";
import type { FriendLinkView } from "./types";

export function useFriendLinks() {
  const [links, setLinks] = useState<FriendLinkView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchFriendLinks();
        if (!active) return;
        setLinks(result);
        setLoading(false);
      } catch (loadError) {
        if (!active) return;
        setLinks([]);
        setError(loadError);
        setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, []);

  return { links, loading, error };
}
