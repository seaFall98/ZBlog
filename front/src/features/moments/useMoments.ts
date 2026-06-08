import { useEffect, useState } from "react";
import { fetchMoments } from "./momentsApi";
import type { MomentView } from "./types";

export function useMoments(pageSize = 30) {
  const [moments, setMoments] = useState<MomentView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    let active = true;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchMoments(pageSize);
        if (!active) return;
        setMoments(result);
        setLoading(false);
      } catch (loadError) {
        if (!active) return;
        setMoments([]);
        setError(loadError);
        setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [pageSize]);

  return { moments, loading, error };
}
