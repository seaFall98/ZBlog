import { useQuery } from "@tanstack/react-query";
import { fetchFriendLinks } from "./linksApi";

export function useFriendLinks() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["friendLinks"],
    queryFn: fetchFriendLinks,
  });

  return {
    links: data?.links ?? [],
    types: data?.types ?? [],
    loading: isLoading,
    error,
  };
}
