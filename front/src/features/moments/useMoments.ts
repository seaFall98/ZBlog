import { useQuery } from "@tanstack/react-query";
import { fetchMoments } from "./momentsApi";
import type { MomentView } from "./types";

export function useMoments(pageSize = 30) {
  const { data, isLoading, error } = useQuery({
    queryKey: ["moments", pageSize],
    queryFn: () => fetchMoments(pageSize),
  });

  return {
    moments: data ?? [],
    loading: isLoading,
    error,
  };
}

export type { MomentView };
