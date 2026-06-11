import { useQuery } from "@tanstack/react-query";
import { fetchMoments } from "./momentsApi";
import type { MomentView } from "./types";

type UseMomentsState = {
  moments: MomentView[];
  loading: boolean;
  error: unknown;
};

export function useMoments(pageSize = 30): UseMomentsState {
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
