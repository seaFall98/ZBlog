import { useQuery } from "@tanstack/react-query";
import { fetchArchiveStats } from "./statsApi";

export function useArchiveStats() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["archiveStats"],
    queryFn: fetchArchiveStats,
  });

  return {
    archiveYears: data ?? [],
    loading: isLoading,
    error,
  };
}
