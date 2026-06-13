import { useQuery } from "@tanstack/react-query";
import { fetchGuestbookMessages } from "./guestbookApi";

export function useGuestbookMessages(page = 1, pageSize = 50) {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ["guestbookMessages", page, pageSize],
    queryFn: () => fetchGuestbookMessages(page, pageSize),
  });

  return {
    messages: data?.messages ?? [],
    total: data?.total ?? 0,
    page: data?.page ?? page,
    pageSize: data?.pageSize ?? pageSize,
    loading: isLoading,
    error,
    reload: refetch,
  };
}
