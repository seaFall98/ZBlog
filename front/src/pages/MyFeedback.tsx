import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ArrowLeftIcon, CheckCircle2Icon, Clock3Icon, Loader2Icon, MessageSquarePlusIcon, Trash2Icon } from "lucide-react";
import { toast } from "sonner";
import PageLayout from "../components/layout/PageLayout";
import { useAuth } from "../features/auth/AuthProvider";
import { feedbackApi } from "../features/feedback/feedbackApi";
import { forgetFeedback, readFeedbackHistory, rememberFeedback, type StoredFeedbackAccess } from "../features/feedback/feedbackHistory";
import type { FeedbackMessage, FeedbackStatus, FeedbackTicket } from "../features/feedback/types";

const STATUS_META: Record<FeedbackStatus, { label: string; tone: string }> = {
  PENDING: { label: "待处理", tone: "var(--clay)" },
  IN_PROGRESS: { label: "处理中", tone: "var(--slate-blue)" },
  WAITING_USER: { label: "待你补充", tone: "var(--clay)" },
  RESOLVED: { label: "已解决", tone: "var(--olive)" },
  CLOSED: { label: "已关闭", tone: "var(--muted-ink)" },
};

function formatDate(value?: string | null) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function messageTitle(message: FeedbackMessage) {
  if (message.message_type === "STATUS_CHANGE") {
    const toStatus = message.to_status ? STATUS_META[message.to_status]?.label ?? message.to_status : "状态更新";
    return `状态变更为 ${toStatus}`;
  }
  if (message.actor_type === "ADMIN") return "管理员回复";
  if (message.actor_type === "SYSTEM") return "系统记录";
  return "你的补充";
}

function sortTickets(tickets: FeedbackTicket[]) {
  return [...tickets].sort((left, right) => {
    const leftTime = new Date(left.updated_at ?? left.feedback_time).getTime();
    const rightTime = new Date(right.updated_at ?? right.feedback_time).getTime();
    return rightTime - leftTime;
  });
}

export default function MyFeedback() {
  const { authenticated, initializing } = useAuth();
  const [searchParams] = useSearchParams();
  const [tickets, setTickets] = useState<FeedbackTicket[]>([]);
  const [history, setHistory] = useState<StoredFeedbackAccess[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [lookupToken, setLookupToken] = useState("");
  const [reply, setReply] = useState("");
  const [replying, setReplying] = useState(false);

  const selectedTicket = useMemo(() => tickets.find((ticket) => ticket.id === selectedId) ?? tickets[0] ?? null, [selectedId, tickets]);
  const selectedAccess = selectedTicket?.access_token ?? history.find((item) => item.ticketNo === selectedTicket?.ticket_no)?.accessToken;
  const canReply = selectedTicket ? selectedTicket.status !== "CLOSED" : false;

  const loadTickets = useCallback(async () => {
    if (initializing) return;
    setLoading(true);
    try {
      const localHistory = readFeedbackHistory();
      setHistory(localHistory);

      const loaded: FeedbackTicket[] = [];
      if (authenticated) {
        const mine = await feedbackApi.mine({ pageSize: 50 });
        loaded.push(...mine.list);
      }

      const localResults = await Promise.allSettled(localHistory.map((item) => feedbackApi.byToken(item.accessToken)));
      for (const result of localResults) {
        if (result.status === "fulfilled") loaded.push(result.value);
      }

      const unique = new Map<number, FeedbackTicket>();
      loaded.forEach((ticket) => unique.set(ticket.id, ticket));
      const sorted = sortTickets(Array.from(unique.values()));
      setTickets(sorted);

      const ticketQuery = searchParams.get("ticket");
      const selected = ticketQuery ? sorted.find((ticket) => ticket.ticket_no === ticketQuery) : sorted[0];
      setSelectedId(selected?.id ?? null);
    } catch {
      toast.error("反馈列表加载失败");
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, [authenticated, initializing, searchParams]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  async function handleLookup(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const token = lookupToken.trim();
    if (!token) return;
    try {
      const ticket = await feedbackApi.byToken(token);
      rememberFeedback(ticket);
      setHistory(readFeedbackHistory());
      setTickets((current) => sortTickets([...current.filter((item) => item.id !== ticket.id), ticket]));
      setSelectedId(ticket.id);
      setLookupToken("");
      toast.success("工单已加入当前浏览器记录");
    } catch {
      toast.error("没有找到这个访问密钥对应的工单");
    }
  }

  async function handleReply(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedTicket || !reply.trim()) return;
    setReplying(true);
    try {
      const updated = await feedbackApi.addMessage(selectedTicket.id, {
        content: reply.trim(),
        access_token: selectedAccess,
      });
      rememberFeedback(updated);
      setTickets((current) => sortTickets(current.map((ticket) => (ticket.id === updated.id ? updated : ticket))));
      setReply("");
      toast.success("补充内容已发送");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "发送失败");
    } finally {
      setReplying(false);
    }
  }

  function handleForget(token?: string) {
    if (!token) return;
    forgetFeedback(token);
    const nextHistory = readFeedbackHistory();
    setHistory(nextHistory);
    if (!authenticated) {
      const allowed = new Set(nextHistory.map((item) => item.accessToken));
      setTickets((current) => current.filter((ticket) => ticket.access_token && allowed.has(ticket.access_token)));
    }
  }

  return (
    <PageLayout>
      <div className="mx-auto max-w-7xl px-6 pb-24 pt-14 md:px-8">
        <div className="mb-10 flex flex-wrap items-end justify-between gap-5">
          <div>
            <Link to="/feedback" className="mb-5 inline-flex items-center gap-2 text-sm" style={{ color: "var(--muted-ink)" }}>
              <ArrowLeftIcon size={15} />
              返回反馈入口
            </Link>
            <h1 style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px, 4vw, 58px)", fontWeight: 400 }}>
              我的反馈
            </h1>
          </div>

          <form onSubmit={handleLookup} className="flex w-full max-w-md gap-2">
            <input
              value={lookupToken}
              onChange={(event) => setLookupToken(event.target.value)}
              className="h-11 min-w-0 flex-1 border bg-white px-3 text-sm outline-none focus:border-[var(--olive)]"
              style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
              placeholder="访问密钥"
            />
            <button type="submit" className="h-11 shrink-0 border px-4 text-sm" style={{ borderColor: "var(--ink)", background: "var(--ink)", color: "var(--warm-white)" }}>
              查询
            </button>
          </form>
        </div>

        <div className="grid gap-6 lg:grid-cols-[360px_minmax(0,1fr)]">
          <aside className="border bg-white" style={{ borderColor: "var(--warm-border)" }}>
            <div className="border-b p-5" style={{ borderColor: "var(--warm-border)" }}>
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium" style={{ color: "var(--ink)" }}>
                  工单列表
                </span>
                <span className="text-xs" style={{ color: "var(--muted-ink)" }}>
                  {tickets.length} 条
                </span>
              </div>
            </div>

            {loading ? (
              <div className="grid gap-3 p-5">
                {[0, 1, 2].map((item) => (
                  <div key={item} className="h-24 animate-pulse bg-[var(--section-bg)]" />
                ))}
              </div>
            ) : tickets.length === 0 ? (
              <div className="p-8 text-center">
                <Clock3Icon className="mx-auto" size={28} style={{ color: "var(--muted-ink)" }} />
                <p className="mt-4 text-sm" style={{ color: "var(--ink)" }}>
                  暂无反馈记录
                </p>
              </div>
            ) : (
              <div className="max-h-[680px] overflow-y-auto">
                {tickets.map((ticket) => {
                  const meta = STATUS_META[ticket.status] ?? STATUS_META.PENDING;
                  const token = ticket.access_token ?? history.find((item) => item.ticketNo === ticket.ticket_no)?.accessToken;
                  return (
                    <button
                      key={ticket.id}
                      type="button"
                      className="block w-full border-b p-5 text-left transition hover:bg-[var(--ivory)]"
                      style={{
                        borderColor: "var(--warm-border)",
                        background: selectedTicket?.id === ticket.id ? "var(--ivory)" : "transparent",
                      }}
                      onClick={() => setSelectedId(ticket.id)}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="truncate text-sm font-medium" style={{ color: "var(--ink)" }}>
                            {ticket.ticket_no}
                          </div>
                          <div className="mt-2 line-clamp-2 text-xs leading-5" style={{ color: "var(--muted-ink)" }}>
                            {ticket.form_content?.description || ticket.report_url}
                          </div>
                        </div>
                        <span className="shrink-0 border px-2 py-1 text-[11px]" style={{ borderColor: meta.tone, color: meta.tone }}>
                          {ticket.status_label ?? meta.label}
                        </span>
                      </div>
                      <div className="mt-4 flex items-center justify-between text-[11px]" style={{ color: "var(--muted-ink)" }}>
                        <span>{formatDate(ticket.updated_at ?? ticket.feedback_time)}</span>
                        {token && !authenticated && (
                          <span
                            role="button"
                            tabIndex={0}
                            className="inline-flex items-center gap-1 hover:text-[var(--ink)]"
                            onClick={(event) => {
                              event.stopPropagation();
                              handleForget(token);
                            }}
                            onKeyDown={(event) => {
                              if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                event.stopPropagation();
                                handleForget(token);
                              }
                            }}
                          >
                            <Trash2Icon size={12} />
                            移除
                          </span>
                        )}
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </aside>

          <section className="min-h-[620px] border bg-white p-5 shadow-custom sm:p-7" style={{ borderColor: "var(--warm-border)" }}>
            {!selectedTicket ? (
              <div className="grid h-full min-h-[460px] place-items-center text-center">
                <div>
                  <MessageSquarePlusIcon className="mx-auto" size={32} style={{ color: "var(--muted-ink)" }} />
                  <p className="mt-4 text-sm" style={{ color: "var(--ink)" }}>
                    选择一个工单查看时间线
                  </p>
                </div>
              </div>
            ) : (
              <div>
                <div className="flex flex-wrap items-start justify-between gap-5 border-b pb-6" style={{ borderColor: "var(--warm-border)" }}>
                  <div>
                    <div className="flex flex-wrap items-center gap-3">
                      <h2 className="text-2xl font-normal" style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)" }}>
                        {selectedTicket.ticket_no}
                      </h2>
                      <span
                        className="border px-2.5 py-1 text-xs"
                        style={{
                          borderColor: STATUS_META[selectedTicket.status]?.tone ?? "var(--warm-border)",
                          color: STATUS_META[selectedTicket.status]?.tone ?? "var(--muted-ink)",
                        }}
                      >
                        {selectedTicket.status_label ?? STATUS_META[selectedTicket.status]?.label ?? selectedTicket.status}
                      </span>
                    </div>
                  </div>
                  <div className="text-right text-xs leading-6" style={{ color: "var(--muted-ink)" }}>
                    <div>提交：{formatDate(selectedTicket.feedback_time)}</div>
                    <div>更新：{formatDate(selectedTicket.updated_at)}</div>
                  </div>
                </div>

                <div className="grid gap-4 border-b py-6 text-sm" style={{ borderColor: "var(--warm-border)" }}>
                  <div>
                    <div className="mb-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                      相关地址
                    </div>
                    <a className="break-all hover:underline" href={selectedTicket.report_url} target="_blank" rel="noreferrer" style={{ color: "var(--ink)" }}>
                      {selectedTicket.report_url}
                    </a>
                  </div>
                  <div>
                    <div className="mb-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                      问题描述
                    </div>
                    <p className="whitespace-pre-wrap leading-7" style={{ color: "var(--ink)" }}>
                      {selectedTicket.form_content?.description}
                    </p>
                  </div>
                </div>

                <div className="py-7">
                  <h3 className="mb-5 text-sm font-medium" style={{ color: "var(--ink)" }}>
                    处理时间线
                  </h3>
                  <div className="grid gap-5">
                    {(selectedTicket.messages ?? []).map((message) => (
                      <div key={message.id} className="grid grid-cols-[18px_minmax(0,1fr)] gap-4">
                        <div className="pt-1">
                          <span
                            className="block h-3.5 w-3.5 rounded-full border"
                            style={{
                              borderColor: message.actor_type === "ADMIN" ? "var(--olive)" : "var(--warm-border)",
                              background: message.actor_type === "ADMIN" ? "var(--olive)" : "var(--warm-white)",
                            }}
                          />
                        </div>
                        <div className="border-b pb-5" style={{ borderColor: "var(--warm-border)" }}>
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <span className="text-sm font-medium" style={{ color: "var(--ink)" }}>
                              {messageTitle(message)}
                            </span>
                            <span className="text-xs" style={{ color: "var(--muted-ink)" }}>
                              {formatDate(message.created_at)}
                            </span>
                          </div>
                          <p className="mt-2 whitespace-pre-wrap text-sm leading-7" style={{ color: "var(--muted-ink)" }}>
                            {message.content}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <form onSubmit={handleReply} className="border p-4" style={{ borderColor: "var(--warm-border)", background: "var(--ivory)" }}>
                  <label className="grid gap-2">
                    <span className="text-sm font-medium" style={{ color: "var(--ink)" }}>
                      补充回复
                    </span>
                    <textarea
                      value={reply}
                      onChange={(event) => setReply(event.target.value)}
                      disabled={!canReply}
                      className="min-h-28 resize-y border bg-white px-3 py-3 text-sm leading-7 outline-none focus:border-[var(--olive)] disabled:cursor-not-allowed disabled:opacity-60"
                      style={{ borderColor: "var(--warm-border)", color: "var(--ink)" }}
                      placeholder={canReply ? "补充说明" : "工单已关闭"}
                    />
                  </label>
                  <button
                    type="submit"
                    disabled={!canReply || replying || !reply.trim()}
                    className="mt-3 inline-flex h-10 items-center gap-2 border px-4 text-sm transition enabled:hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
                    style={{ borderColor: "var(--ink)", background: "var(--ink)", color: "var(--warm-white)" }}
                  >
                    {replying ? <Loader2Icon className="animate-spin" size={15} /> : <CheckCircle2Icon size={15} />}
                    发送补充
                  </button>
                </form>
              </div>
            )}
          </section>
        </div>
      </div>
    </PageLayout>
  );
}
