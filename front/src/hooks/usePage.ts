import { useCallback, useEffect } from "react";
import { useSearchParams } from "react-router-dom";

export function usePage(defaultPage = 1) {
  const [searchParams, setSearchParams] = useSearchParams();
  const rawPage = Number(searchParams.get("page"));
  const page = Number.isFinite(rawPage) && rawPage > 0 ? Math.floor(rawPage) : defaultPage;

  const setPage = useCallback(
    (nextPage: number) => {
      const next = new URLSearchParams(searchParams);
      const normalized = Math.max(1, Math.floor(nextPage));
      if (normalized <= 1) {
        next.delete("page");
      } else {
        next.set("page", String(normalized));
      }
      setSearchParams(next, { replace: true });
    },
    [searchParams, setSearchParams],
  );

  return { page, setPage };
}

export function useNormalizePage(page: number, setPage: (page: number) => void, totalPages: number, loading = false) {
  useEffect(() => {
    if (loading) return;
    const normalizedTotalPages = Math.max(1, Math.floor(totalPages));
    if (page > normalizedTotalPages) {
      setPage(normalizedTotalPages);
    }
  }, [loading, page, setPage, totalPages]);
}
