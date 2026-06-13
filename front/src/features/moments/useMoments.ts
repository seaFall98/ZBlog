import { useQuery } from "@tanstack/react-query";
import { fetchMoments } from "./momentsApi";
import type { MomentView } from "./types";

export function useMoments(page = 1, pageSize = 30) {
  const { data, isLoading, error } = useQuery({
    queryKey: ["moments", page, pageSize],
    queryFn: () => fetchMoments(page, pageSize),
  });

  return {
    moments: data?.moments ?? [],
    total: data?.total ?? 0,
    page: data?.page ?? page,
    pageSize: data?.pageSize ?? pageSize,
    loading: isLoading,
    error,
  };
}

export type { MomentView };
