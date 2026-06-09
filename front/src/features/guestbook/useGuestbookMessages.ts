import { useCallback, useEffect, useState } from "react";
import { fetchGuestbookMessages } from "./guestbookApi";
import type { GuestbookMessageView } from "./types";

export function useGuestbookMessages(pageSize = 50) {
  const [messages, setMessages] = useState<GuestbookMessageView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchGuestbookMessages(pageSize);
      setMessages(result);
      setLoading(false);
    } catch (loadError) {
      setMessages([]);
      setError(loadError);
      setLoading(false);
    }
  }, [pageSize]);

  useEffect(() => {
    queueMicrotask(() => { void reload(); });
  }, [reload]);

  return { messages, loading, error, reload };
}
