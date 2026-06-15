import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowRightIcon,
  BellIcon,
  CheckCheckIcon,
  InboxIcon,
  MailOpenIcon,
  MessageCircleIcon,
} from "lucide-react";
import { toast } from "sonner";
import PageLayout from "../components/layout/PageLayout";
import { notificationApi } from "../features/notifications/notificationApi";
import type { NotificationItem } from "../features/notifications/notificationTypes";
import { ApiEnvelopeError } from "../lib/apiEnvelope";
import { ApiHttpError } from "../lib/apiClient";

const PAGE_SIZE = 10;
export const NOTIFICATION_READ_EVENT = "zblog:notifications-read";

function errorMessage(error: unknown) {
  if (error instanceof ApiEnvelopeError) return error.message || "请求失败";
  if (error instanceof ApiHttpError) return error.message || "请求失败";
  return "请求失败";
}

function formatTime(value: string) {
  if (!value) return "";
  const date = new Date(value.replace(" ", "T"));
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function notifyUnreadChanged() {
  window.dispatchEvent(new Event(NOTIFICATION_READ_EVENT));
}

function NotificationIcon({ item }: { item: NotificationItem }) {
  const Icon = item.type === "comment_reply" ? MessageCircleIcon : BellIcon;
  return (
    <span
      className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border"
      style={{
        borderColor: item.isRead ? "var(--warm-border)" : "var(--ink)",
        background: item.isRead ? "var(--warm-white)" : "var(--ink)",
        color: item.isRead ? "var(--muted-ink)" : "var(--warm-white)",
      }}
    >
      <Icon size={17} />
    </span>
  );
}

function SkeletonList() {
  return (
    <div className="divide-y border bg-card" style={{ borderColor: "var(--warm-border)" }}>
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="flex gap-4 p-5">
          <div className="h-10 w-10 rounded-full" style={{ background: "var(--section-bg)" }} />
          <div className="flex-1 space-y-3">
            <div className="h-4 w-40" style={{ background: "var(--section-bg)" }} />
            <div className="h-3 w-full max-w-xl" style={{ background: "var(--section-bg)" }} />
          </div>
        </div>
      ))}
    </div>
  );
}

export default function Notifications() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<"all" | "unread">("all");
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const hasMore = items.length < total;
  const activeLabel = useMemo(() => (filter === "unread" ? "未读" : "全部"), [filter]);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setPage(1);
    notificationApi
      .list({ page: 1, pageSize: PAGE_SIZE, unreadOnly: filter === "unread" })
      .then((result) => {
        if (!alive) return;
        setItems(result.list);
        setTotal(result.total);
        setUnreadCount(result.unreadCount);
      })
      .catch((error) => {
        if (alive) toast.error(errorMessage(error));
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [filter]);

  const loadMore = async () => {
    const nextPage = page + 1;
    setLoadingMore(true);
    try {
      const result = await notificationApi.list({
        page: nextPage,
        pageSize: PAGE_SIZE,
        unreadOnly: filter === "unread",
      });
      setItems((current) => {
        const known = new Set(current.map((item) => item.id));
        return [...current, ...result.list.filter((item) => !known.has(item.id))];
      });
      setPage(nextPage);
      setTotal(result.total);
      setUnreadCount(result.unreadCount);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setLoadingMore(false);
    }
  };

  const markAllRead = async () => {
    if (unreadCount === 0) return;
    try {
      await notificationApi.markAllRead();
      setUnreadCount(0);
      setItems((current) => current.map((item) => ({ ...item, isRead: true })));
      notifyUnreadChanged();
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const openNotification = async (item: NotificationItem) => {
    if (!item.isRead) {
      setItems((current) => current.map((row) => (row.id === item.id ? { ...row, isRead: true } : row)));
      setUnreadCount((count) => Math.max(0, count - 1));
      try {
        await notificationApi.markRead(item.id);
        notifyUnreadChanged();
      } catch (error) {
        toast.error(errorMessage(error));
      }
    }

    if (item.link) {
      navigate(item.link);
    }
  };

  return (
    <PageLayout>
      <section className="px-5 py-16 md:px-8 md:py-20" style={{ background: "var(--ivory)" }}>
        <div className="mx-auto max-w-5xl">
          <header className="border-b pb-8" style={{ borderColor: "var(--warm-border)" }}>
            <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
              <div>
                <p className="text-sm" style={{ color: "var(--muted-ink)" }}>
                  Notifications
                </p>
                <h1
                  className="mt-3 text-4xl leading-tight md:text-5xl"
                  style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", letterSpacing: "0" }}
                >
                  通知中心
                </h1>
              </div>
              <div className="flex items-center gap-3">
                <span
                  className="inline-flex h-10 items-center gap-2 rounded-full border px-4 text-sm"
                  style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)", color: "var(--ink)" }}
                >
                  <BellIcon size={15} />
                  {unreadCount} 条未读
                </span>
                <button
                  type="button"
                  onClick={markAllRead}
                  disabled={unreadCount === 0}
                  className="inline-flex h-10 items-center gap-2 px-4 text-sm transition-opacity hover:opacity-85 disabled:opacity-45"
                  style={{ background: "var(--ink)", color: "var(--warm-white)" }}
                >
                  <CheckCheckIcon size={15} />
                  全部已读
                </button>
              </div>
            </div>
          </header>

          <div className="mt-8 flex items-center justify-between gap-4">
            <div className="inline-flex border p-1" style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)" }}>
              {(["all", "unread"] as const).map((key) => {
                const active = filter === key;
                return (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setFilter(key)}
                    className="h-9 px-4 text-sm transition-colors"
                    style={{
                      background: active ? "var(--ink)" : "transparent",
                      color: active ? "var(--warm-white)" : "var(--muted-ink)",
                    }}
                  >
                    {key === "all" ? "全部" : "未读"}
                  </button>
                );
              })}
            </div>
            <span className="hidden text-sm md:inline" style={{ color: "var(--muted-ink)" }}>
              当前查看：{activeLabel}
            </span>
          </div>

          <div className="mt-6">
            {loading ? (
              <SkeletonList />
            ) : items.length === 0 ? (
              <div
                className="border bg-card px-6 py-16 text-center"
                style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)" }}
              >
                <InboxIcon size={34} className="mx-auto mb-4" style={{ color: "var(--warm-border)" }} />
                <p className="text-base" style={{ color: "var(--ink)" }}>
                  没有{filter === "unread" ? "未读" : ""}通知
                </p>
              </div>
            ) : (
              <div className="divide-y border bg-card" style={{ borderColor: "var(--warm-border)" }}>
                {items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => openNotification(item)}
                    className="group flex w-full items-start gap-4 px-5 py-5 text-left transition-colors hover:bg-section-bg"
                    style={{ borderColor: "var(--warm-border)" }}
                  >
                    <NotificationIcon item={item} />
                    <span className="min-w-0 flex-1">
                      <span className="mb-2 flex flex-wrap items-center gap-2">
                        <span className="text-base font-medium" style={{ color: "var(--ink)" }}>
                          {item.title || item.typeText}
                        </span>
                        {!item.isRead && (
                          <span className="rounded-full px-2 py-0.5 text-[11px]" style={{ background: "var(--ink)", color: "var(--warm-white)" }}>
                            未读
                          </span>
                        )}
                        {item.isRead && (
                          <span className="inline-flex items-center gap-1 text-[11px]" style={{ color: "var(--muted-ink)" }}>
                            <MailOpenIcon size={12} />
                            已读
                          </span>
                        )}
                      </span>
                      <span className="block text-sm leading-6" style={{ color: "var(--muted-ink)" }}>
                        {item.content}
                      </span>
                      <span className="mt-3 flex items-center gap-3 text-xs" style={{ color: "var(--muted-ink)" }}>
                        <span>{item.typeText}</span>
                        <span>{formatTime(item.createdAt)}</span>
                      </span>
                    </span>
                    <ArrowRightIcon
                      size={16}
                      className="mt-1 shrink-0 opacity-45 transition-transform group-hover:translate-x-0.5 group-hover:opacity-100"
                      style={{ color: "var(--ink)" }}
                    />
                  </button>
                ))}
              </div>
            )}

            {hasMore && (
              <div className="mt-8 flex justify-center">
                <button
                  type="button"
                  onClick={loadMore}
                  disabled={loadingMore}
                  className="h-11 border px-6 text-sm transition-colors hover:bg-secondary disabled:opacity-50"
                  style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                >
                  {loadingMore ? "加载中..." : "加载更多"}
                </button>
              </div>
            )}
          </div>
        </div>
      </section>
    </PageLayout>
  );
}
