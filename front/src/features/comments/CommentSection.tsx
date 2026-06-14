import { type FormEvent, type ReactNode, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ChevronDownIcon, ChevronUpIcon, MessageCircleIcon, SendIcon, ThumbsUpIcon, Trash2Icon, XIcon } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../auth/AuthProvider";
import { deleteComment, fetchCommentPage, fetchReplies, locateComment, submitComment } from "./commentApi";
import { CommentMarkdown } from "./markdown";
import type { CommentPage, CommentView } from "./types";

type CommentSectionProps = {
  targetType: string;
  targetKey: string;
  compact?: boolean;
};

type ReplyPageState = {
  list: CommentView[];
  total: number;
  page: number;
  pageSize: number;
};

const ROOT_PAGE_SIZE = 10;
const REPLY_PAGE_SIZE = 10;
const PAGE_WINDOW = 5;

function countComments(comments: CommentView[]): number {
  return comments.reduce((sum, comment) => sum + 1 + comment.replyTotal, 0);
}

function dateText(value: string) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("zh-CN", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

function totalPages(total: number, pageSize: number) {
  return total > 0 ? Math.ceil(total / pageSize) : 0;
}

function pageNumbers(current: number, total: number) {
  if (total <= PAGE_WINDOW + 2) return Array.from({ length: total }, (_, index) => index + 1);
  const start = Math.max(1, Math.min(current - 2, total - PAGE_WINDOW + 1));
  const pages = Array.from({ length: PAGE_WINDOW }, (_, index) => start + index);
  if (!pages.includes(1)) pages.unshift(1);
  if (!pages.includes(total)) pages.push(total);
  return pages;
}

function CommentAvatar({ comment }: { comment: CommentView }) {
  const initial = (comment.nickname || "访").slice(0, 1);
  return (
    <div className="h-10 w-10 shrink-0 overflow-hidden rounded-full border" style={{ borderColor: "var(--warm-border)", background: "var(--section-bg)" }}>
      {comment.avatar ? (
        <img src={comment.avatar} alt="" className="h-full w-full object-cover" />
      ) : (
        <div className="flex h-full w-full items-center justify-center text-sm" style={{ color: "var(--ink)", fontFamily: "var(--fontDisplay)" }}>
          {initial}
        </div>
      )}
    </div>
  );
}

function Composer({
  value,
  onChange,
  onSubmit,
  onCancel,
  placeholder,
  submitting,
  compact,
}: {
  value: string;
  onChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
  onCancel?: () => void;
  placeholder: string;
  submitting: boolean;
  compact?: boolean;
}) {
  return (
    <form onSubmit={onSubmit} className="border bg-card p-4 shadow-sm" style={{ borderColor: "var(--warm-border)" }}>
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        rows={compact ? 3 : 4}
        maxLength={2000}
        placeholder={placeholder}
        className="w-full resize-none border bg-transparent px-3 py-3 text-sm leading-6 outline-none transition-colors focus:border-current"
        style={{ borderColor: "var(--warm-border)", color: "var(--ink)", fontFamily: "var(--fontBody)" }}
      />
      <div className="mt-3 flex items-center justify-between gap-3">
        <span className="text-xs" style={{ color: "var(--muted-ink)" }}>
          {value.length}/2000
        </span>
        <div className="flex items-center gap-2">
          {onCancel && (
            <button type="button" onClick={onCancel} className="inline-flex h-10 items-center justify-center gap-2 border px-4 text-sm transition-opacity hover:opacity-80" style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)" }}>
              <XIcon size={14} />
              取消
            </button>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="inline-flex h-10 items-center justify-center gap-2 px-4 text-sm font-medium transition-opacity hover:opacity-85 disabled:opacity-50"
            style={{ background: "var(--ink)", color: "var(--warm-white)" }}
          >
            <SendIcon size={15} />
            {submitting ? "发布中..." : "发布"}
          </button>
        </div>
      </div>
    </form>
  );
}

function CommentItem({
  comment,
  currentUserId,
  onReply,
  onDelete,
  replyComposer,
  compact,
  isReply = false,
}: {
  comment: CommentView;
  currentUserId: number;
  onReply: (comment: CommentView) => void;
  onDelete: (comment: CommentView) => void;
  replyComposer?: ReactNode;
  compact?: boolean;
  isReply?: boolean;
}) {
  const [expanded, setExpanded] = useState(false);
  const canDelete = currentUserId > 0 && comment.userId === currentUserId && !comment.isDeleted;
  const isLong = comment.content.length > 300;
  const visibleContent = isLong && !expanded ? `${comment.content.slice(0, 300)}...` : comment.content;

  return (
    <article id={`comment-${comment.id}`} className={isReply ? "py-4" : compact ? "py-5" : "py-6"}>
      <div className="flex gap-3">
        <CommentAvatar comment={comment} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm font-medium" style={{ color: "var(--ink)" }}>
              {comment.nickname}
            </span>
            {comment.badge && !comment.isDeleted && (
              <span className="rounded-full border px-2 py-0.5 text-[11px]" style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)" }}>
                {comment.badge}
              </span>
            )}
            <span className="text-xs" style={{ color: "var(--muted-ink)" }}>
              {dateText(comment.createdAt)}
            </span>
          </div>

          <div className="mt-2 text-sm leading-7" style={{ color: "var(--ink)" }}>
            {comment.replyUser && !comment.isDeleted && (
              <span className="mr-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                回复 @{comment.replyUser.nickname}
              </span>
            )}
            <CommentMarkdown content={visibleContent} />
          </div>

          {!comment.isDeleted && (
            <div className="mt-3 flex flex-wrap items-center gap-4">
              {isLong && (
                <button type="button" onClick={() => setExpanded((value) => !value)} className="text-xs transition-opacity hover:opacity-70" style={{ color: "var(--muted-ink)" }}>
                  {expanded ? "收起全文" : "展开全文"}
                </button>
              )}
              <button type="button" className="inline-flex items-center gap-1 text-xs transition-opacity hover:opacity-70" style={{ color: "var(--muted-ink)" }} aria-disabled="true">
                <ThumbsUpIcon size={13} />
                {comment.likeCount > 0 ? comment.likeCount : "赞"}
              </button>
              <button type="button" onClick={() => onReply(comment)} className="text-xs transition-opacity hover:opacity-70" style={{ color: "var(--muted-ink)" }}>
                回复
              </button>
              {canDelete && (
                <button type="button" onClick={() => onDelete(comment)} className="inline-flex items-center gap-1 text-xs transition-opacity hover:opacity-70" style={{ color: "#9A3A2F" }}>
                  <Trash2Icon size={12} />
                  删除
                </button>
              )}
            </div>
          )}

          {replyComposer && <div className="mt-4">{replyComposer}</div>}
        </div>
      </div>
    </article>
  );
}

function ReplyPager({
  state,
  onPageChange,
  onCollapse,
}: {
  state: ReplyPageState;
  onPageChange: (page: number) => void;
  onCollapse: () => void;
}) {
  const pages = totalPages(state.total, state.pageSize);
  if (pages <= 0) return null;
  const numbers = pageNumbers(state.page, pages);
  return (
    <div className="mt-3 flex flex-wrap items-center gap-2 text-xs" style={{ color: "var(--muted-ink)" }}>
      <span>共{pages}页</span>
      {numbers.map((page, index) => (
        <span key={`${page}-${index}`} className="inline-flex items-center gap-2">
          {index > 0 && page - numbers[index - 1] > 1 && <span>...</span>}
          <button type="button" onClick={() => onPageChange(page)} className="px-1 transition-opacity hover:opacity-70" style={{ color: page === state.page ? "var(--ink)" : "var(--muted-ink)" }}>
            {page}
          </button>
        </span>
      ))}
      {state.page < pages && (
        <button type="button" onClick={() => onPageChange(state.page + 1)} className="transition-opacity hover:opacity-70" style={{ color: "var(--ink)" }}>
          下一页
        </button>
      )}
      <button type="button" onClick={onCollapse} className="transition-opacity hover:opacity-70" style={{ color: "var(--ink)" }}>
        收起
      </button>
    </div>
  );
}

export default function CommentSection({ targetType, targetKey, compact = false }: CommentSectionProps) {
  const { authenticated, user } = useAuth();
  const [searchParams] = useSearchParams();
  const [comments, setComments] = useState<CommentView[]>([]);
  const [rootPage, setRootPage] = useState(1);
  const [rootTotal, setRootTotal] = useState(0);
  const [rootContent, setRootContent] = useState("");
  const [replyContent, setReplyContent] = useState("");
  const [replyTo, setReplyTo] = useState<CommentView | null>(null);
  const [expandedRoots, setExpandedRoots] = useState<Record<string, boolean>>({});
  const [replyPages, setReplyPages] = useState<Record<string, ReplyPageState>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [pendingScrollId, setPendingScrollId] = useState<string | null>(null);
  const total = useMemo(() => countComments(comments), [comments]);
  const rootTotalPages = totalPages(rootTotal, ROOT_PAGE_SIZE);

  const loadRoots = async (page = rootPage): Promise<CommentPage> => {
    setLoading(true);
    try {
      const result = await fetchCommentPage(targetType, targetKey, page, ROOT_PAGE_SIZE, REPLY_PAGE_SIZE);
      setComments(result.list);
      setRootTotal(result.total);
      setRootPage(result.page);
      setReplyPages((current) => {
        const next = { ...current };
        result.list.forEach((comment) => {
          next[comment.id] = {
            list: comment.replies,
            total: comment.replyTotal,
            page: comment.replyPage,
            pageSize: comment.replyPageSize || REPLY_PAGE_SIZE,
          };
        });
        return next;
      });
      return result;
    } catch {
      toast.error("评论加载失败");
      return { list: [], total: 0, page, pageSize: ROOT_PAGE_SIZE };
    } finally {
      setLoading(false);
    }
  };

  const loadReplies = async (rootId: string, page = 1) => {
    const result = await fetchReplies(rootId, page, REPLY_PAGE_SIZE);
    setReplyPages((current) => ({
      ...current,
      [rootId]: { list: result.list, total: result.total, page: result.page, pageSize: result.pageSize },
    }));
    setExpandedRoots((current) => ({ ...current, [rootId]: true }));
    return result;
  };

  useEffect(() => {
    void loadRoots(1);
  }, [targetKey, targetType]);

  useEffect(() => {
    const commentId = searchParams.get("commentId");
    if (!commentId) return;
    let active = true;
    locateComment(targetType, targetKey, commentId, ROOT_PAGE_SIZE, REPLY_PAGE_SIZE)
      .then(async (location) => {
        if (!active) return;
        await loadRoots(location.rootPage);
        if (!active) return;
        setExpandedRoots((current) => ({ ...current, [location.rootId]: true }));
        if (!location.isRoot) {
          await loadReplies(location.rootId, location.replyPage);
        }
        if (active) setPendingScrollId(location.commentId);
      })
      .catch(() => {
        if (active) toast.error("未找到对应评论");
      });
    return () => {
      active = false;
    };
  }, [searchParams, targetKey, targetType]);

  useEffect(() => {
    if (!pendingScrollId) return;
    window.setTimeout(() => {
      document.querySelector(`#comment-${pendingScrollId}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
      setPendingScrollId(null);
    }, 80);
  }, [comments, pendingScrollId, replyPages]);

  const submitRoot = async (event: FormEvent) => {
    event.preventDefault();
    if (!authenticated) {
      toast.error("请先登录后评论");
      return;
    }
    const text = rootContent.trim();
    if (!text) {
      toast.error("请填写评论内容");
      return;
    }
    setSubmitting(true);
    try {
      await submitComment({ target_type: targetType, target_key: targetKey, content: text });
      setRootContent("");
      await loadRoots(1);
      toast.success("评论已发布");
    } catch {
      toast.error("评论发布失败");
    } finally {
      setSubmitting(false);
    }
  };

  const submitReply = async (event: FormEvent) => {
    event.preventDefault();
    if (!replyTo) return;
    if (!authenticated) {
      toast.error("请先登录后评论");
      return;
    }
    const text = replyContent.trim();
    if (!text) {
      toast.error("请填写回复内容");
      return;
    }
    setSubmitting(true);
    try {
      const created = await submitComment({
        target_type: targetType,
        target_key: targetKey,
        content: text,
        parent_id: Number(replyTo.id),
      });
      setReplyContent("");
      setReplyTo(null);
      toast.success("回复已发布");
      // Post-submission UI refresh (best-effort — reply was already persisted).
      try {
        const location = await locateComment(targetType, targetKey, created.id, ROOT_PAGE_SIZE, REPLY_PAGE_SIZE);
        await loadRoots(location.rootPage);
        await loadReplies(location.rootId, location.replyPage);
        setPendingScrollId(created.id);
      } catch {
        // Refresh failed but the reply was created; reload from page 1.
        await loadRoots(1);
      }
    } catch {
      toast.error("回复发布失败");
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (comment: CommentView) => {
    if (!window.confirm("确认删除这条评论？")) return;
    try {
      await deleteComment(comment.id);
      await loadRoots(rootPage);
      toast.success("评论已删除");
    } catch {
      toast.error("删除失败");
    }
  };

  const toggleRoot = async (root: CommentView) => {
    if (expandedRoots[root.id]) {
      setExpandedRoots((current) => ({ ...current, [root.id]: false }));
      return;
    }
    setExpandedRoots((current) => ({ ...current, [root.id]: true }));
    if (!replyPages[root.id]) {
      try {
        await loadReplies(root.id, 1);
      } catch {
        toast.error("回复加载失败");
      }
    }
  };

  const replyComposer = (comment: CommentView) =>
    replyTo?.id === comment.id ? (
      <Composer
        value={replyContent}
        onChange={setReplyContent}
        onSubmit={submitReply}
        onCancel={() => {
          setReplyTo(null);
          setReplyContent("");
        }}
        placeholder={`回复 ${comment.nickname}`}
        submitting={submitting}
        compact
      />
    ) : null;

  return (
    <section className={compact ? "mt-5 border-t pt-5" : "mt-16 border-t pt-10"} style={{ borderColor: "var(--warm-border)" }}>
      <div className="mb-5 flex items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <MessageCircleIcon size={18} style={{ color: "var(--ink)" }} />
            <h2 className={compact ? "text-xl" : "text-2xl"} style={{ color: "var(--ink)", letterSpacing: "0" }}>
              评论
            </h2>
          </div>
          <p className="mt-1 text-xs" style={{ color: "var(--muted-ink)" }}>
            {loading ? "正在加载评论..." : total > 0 ? `${total} 条讨论` : "还没有评论"}
          </p>
        </div>
      </div>

      {authenticated ? (
        <Composer value={rootContent} onChange={setRootContent} onSubmit={submitRoot} placeholder="写下你的评论" submitting={submitting} compact={compact} />
      ) : (
        <div className="border bg-card p-4 shadow-sm" style={{ borderColor: "var(--warm-border)" }}>
          <p className="text-sm" style={{ color: "var(--muted-ink)" }}>登录后参与评论</p>
          <Link
            to="/login"
            state={{ from: window.location.pathname + window.location.search + window.location.hash }}
            className="mt-3 inline-flex h-10 items-center justify-center px-4 text-sm font-medium transition-opacity hover:opacity-85"
            style={{ background: "var(--ink)", color: "var(--warm-white)" }}
          >
            登录后评论
          </Link>
        </div>
      )}

      <div className="mt-4 divide-y" style={{ borderColor: "var(--warm-border)" }}>
        {comments.map((root) => {
          const state = replyPages[root.id] ?? { list: root.replies, total: root.replyTotal, page: 1, pageSize: REPLY_PAGE_SIZE };
          const pages = totalPages(state.total, state.pageSize);
          return (
            <div key={root.id}>
              <CommentItem
                comment={root}
                currentUserId={user?.id ?? 0}
                onReply={(comment) => setReplyTo(comment)}
                onDelete={remove}
                replyComposer={replyComposer(root)}
                compact={compact}
              />
              {root.replyTotal > 0 && (
                <div className="ml-14 border-l pb-4 pl-4" style={{ borderColor: "var(--warm-border)" }}>
                  {!expandedRoots[root.id] ? (
                    <button type="button" onClick={() => void toggleRoot(root)} className="inline-flex items-center gap-1 text-xs transition-opacity hover:opacity-70" style={{ color: "var(--muted-ink)" }}>
                      <ChevronDownIcon size={14} />
                      共{root.replyTotal}条回复，点击查看
                    </button>
                  ) : (
                    <>
                      <div className="divide-y" style={{ borderColor: "var(--warm-border)" }}>
                        {state.list.map((reply) => (
                          <CommentItem
                            key={reply.id}
                            comment={reply}
                            currentUserId={user?.id ?? 0}
                            onReply={(comment) => setReplyTo(comment)}
                            onDelete={remove}
                            replyComposer={replyComposer(reply)}
                            compact
                            isReply
                          />
                        ))}
                      </div>
                      {pages > 1 ? (
                        <ReplyPager state={state} onPageChange={(page) => void loadReplies(root.id, page)} onCollapse={() => setExpandedRoots((current) => ({ ...current, [root.id]: false }))} />
                      ) : (
                        <button type="button" onClick={() => setExpandedRoots((current) => ({ ...current, [root.id]: false }))} className="mt-3 inline-flex items-center gap-1 text-xs transition-opacity hover:opacity-70" style={{ color: "var(--muted-ink)" }}>
                          <ChevronUpIcon size={14} />
                          收起
                        </button>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {rootTotalPages > 1 && (
        <div className="mt-6 flex flex-wrap items-center justify-center gap-2 text-xs" style={{ color: "var(--muted-ink)" }}>
          {pageNumbers(rootPage, rootTotalPages).map((page, index, numbers) => (
            <span key={`${page}-${index}`} className="inline-flex items-center gap-2">
              {index > 0 && page - numbers[index - 1] > 1 && <span>...</span>}
              <button type="button" onClick={() => void loadRoots(page)} className="border px-3 py-1.5 transition-colors hover:border-current" style={{ borderColor: page === rootPage ? "var(--ink)" : "var(--warm-border)", color: page === rootPage ? "var(--ink)" : "var(--muted-ink)" }}>
                {page}
              </button>
            </span>
          ))}
        </div>
      )}
    </section>
  );
}
