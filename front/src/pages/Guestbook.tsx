import { useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { SendIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { AppPagination } from "../components/ui/app-pagination";
import { fetchComments, submitComment } from "../features/comments/commentApi";
import type { CommentView } from "../features/comments/types";
import { fetchGuestbookMessages, submitGuestbookMessage } from "../features/guestbook/guestbookApi";
import { useGuestbookMessages } from "../features/guestbook/useGuestbookMessages";
import { useSiteProfile } from "../features/site/useSiteProfile";
import { useNormalizePage, usePage } from "../hooks/usePage";
import { toDateText } from "../lib/text";
import { toast } from "sonner";

interface Danmaku {
  id: number;
  text: string;
  top: number;
  speed: number;
  delay: number;
  color: string;
}

const DANMAKU_COLORS = ["rgba(255,255,255,0.9)", "rgba(245,238,224,0.82)", "rgba(201,174,134,0.78)", "rgba(255,255,255,0.66)"];
const COMMENT_TARGET_TYPE = "page";
const COMMENT_TARGET_KEY = "guestbook";
const DEFAULT_GUESTBOOK_BACKGROUND = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1800&q=85";
const MESSAGE_PAGE_SIZE = 10;

type GuestbookCommentProps = {
  comment: CommentView;
  depth?: number;
  replyingTo: string | null;
  onReply: (comment: CommentView) => void;
};

function GuestbookComment({ comment, depth = 0, replyingTo, onReply }: GuestbookCommentProps) {
  return (
    <article className={`guestbook-comment ${depth > 0 ? "guestbook-comment--reply" : ""}`} id={`comment-${comment.id}`}>
      <img className="guestbook-comment__avatar" src={comment.avatar} alt={comment.nickname} loading="lazy" />
      <div className="guestbook-comment__body">
        <header className="guestbook-comment__header">
          <strong>{comment.nickname}</strong>
          {comment.createdAt && <span>{toDateText(comment.createdAt)}</span>}
        </header>
        <p>{comment.content}</p>
        <button type="button" onClick={() => onReply(comment)}>{replyingTo === comment.id ? "取消回复" : "回复"}</button>
        {comment.replies.length > 0 && (
          <div className="guestbook-comment__replies">
            {comment.replies.map((reply) => (
              <GuestbookComment key={reply.id} comment={reply} depth={depth + 1} replyingTo={replyingTo} onReply={onReply} />
            ))}
          </div>
        )}
      </div>
    </article>
  );
}

export default function Guestbook() {
  const [content, setContent] = useState("");
  const [commentName, setCommentName] = useState("");
  const [commentContent, setCommentContent] = useState("");
  const [replyingTo, setReplyingTo] = useState<CommentView | null>(null);
  const { page, setPage } = usePage();
  const { messages, total, loading, reload } = useGuestbookMessages(page, MESSAGE_PAGE_SIZE);
  const { profile } = useSiteProfile();
  const messageTotalPages = Math.ceil(total / MESSAGE_PAGE_SIZE);
  useNormalizePage(page, setPage, messageTotalPages, loading);

  /* Danmaku pool — separate query with configurable limit */
  const danmakuLimit = profile.guestbookDanmakuLimit || 200;
  const { data: danmakuData } = useQuery({
    queryKey: ["guestbookDanmakuPool", danmakuLimit],
    queryFn: () => fetchGuestbookMessages(1, Math.min(danmakuLimit, 500)),
    staleTime: 60 * 1000,
  });
  const danmakuMessages = danmakuData?.messages ?? [];

  const { data: comments = [], isLoading: commentsLoading, refetch: refetchComments } = useQuery({
    queryKey: ["guestbookComments"],
    queryFn: () => fetchComments(COMMENT_TARGET_TYPE, COMMENT_TARGET_KEY, 50),
  });

  const [submittedDanmakus, setSubmittedDanmakus] = useState<Danmaku[]>([]);
  const danmakuIdRef = useRef(1000);
  const backgroundImage = profile.barrageBackgroundImage || profile.backgroundImage || DEFAULT_GUESTBOOK_BACKGROUND;

  /* Show up to 16 approved danmaku + local submitted ones, limited tracks */
  const MAX_RENDERED_DANMAKU = 16;
  const danmakus = useMemo<Danmaku[]>(() => [
    ...danmakuMessages.slice(0, MAX_RENDERED_DANMAKU).map((m, i) => ({
      id: i,
      text: `${m.name}：${m.content.slice(0, 34)}`,
      top: ((i * 11) % 64) + 12,
      speed: 20 + (i % 5) * 3,
      delay: -((i * 2.7) % 18),
      color: DANMAKU_COLORS[i % DANMAKU_COLORS.length] ?? "rgba(255,255,255,0.78)",
    })),
    ...submittedDanmakus,
  ], [danmakuMessages, submittedDanmakus]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) {
      toast.error("请填写留言内容");
      return;
    }

    try {
      const result = await submitGuestbookMessage({ nickname: "访客", content: content.trim() });
      const newDanmaku: Danmaku = {
        id: ++danmakuIdRef.current,
        text: `访客：${content.slice(0, 34)}`,
        top: ((danmakuIdRef.current * 11) % 64) + 12,
        speed: 17 + (danmakuIdRef.current % 5) * 2,
        delay: 0,
        color: DANMAKU_COLORS[danmakuIdRef.current % DANMAKU_COLORS.length] ?? "rgba(255,255,255,0.78)",
      };
      setSubmittedDanmakus((prev) => [...prev, newDanmaku]);
      setContent("");
      toast.success(result.message || (result.status === "pending" ? "留言已提交，等待审核" : "留言成功"));
      void reload();
    } catch {
      toast.error("留言发送失败，请稍后再试");
    }
  };

  const handleCommentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentName.trim() || !commentContent.trim()) {
      toast.error("请填写昵称和评论内容");
      return;
    }

    try {
      await submitComment({
        target_type: COMMENT_TARGET_TYPE,
        target_key: COMMENT_TARGET_KEY,
        nickname: commentName.trim(),
        content: commentContent.trim(),
        parent_id: replyingTo ? Number(replyingTo.id) : undefined,
      });
      setCommentContent("");
      setReplyingTo(null);
      toast.success("评论已提交");
      void refetchComments();
    } catch {
      toast.error("评论发送失败，请稍后再试");
    }
  };

  return (
    <PageLayout headerVariant="guestbook" noMainTopPadding>
      <section className="guestbook-hero" style={{ backgroundImage: `url(${backgroundImage})` }}>
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
          {danmakus.map((d) => (
            <div
              key={d.id}
              className="guestbook-danmaku"
              style={{ top: `${d.top}%`, color: d.color, animationDuration: `${d.speed}s`, animationDelay: `${d.delay}s` }}
            >
              {d.text}
            </div>
          ))}
        </div>

        <div className="guestbook-hero__content">
          <p>{profile.guestbookIntro || "把想说的话留在这里，让它慢慢飘过留言墙。"}</p>
          <form onSubmit={handleSubmit} className="guestbook-form">
            <button type="submit"><SendIcon size={14} /> 发送</button>
            <input type="text" value={content} onChange={(e) => setContent(e.target.value)} placeholder="留下点什么啦" aria-label="留言内容" />
          </form>
        </div>
      </section>

      <AppPagination page={page} totalPages={messageTotalPages} onPageChange={setPage} className="mb-4" />

      <section className="guestbook-board" aria-label="留言评论区">
        <div className="guestbook-board__heading">
          <h2>留言区</h2>
          <span>{commentsLoading ? "正在加载" : `${comments.length} 条评论`}</span>
        </div>

        <form className="guestbook-comment-form" onSubmit={handleCommentSubmit}>
          {replyingTo && (
            <div className="guestbook-comment-form__replying">
              正在回复 {replyingTo.nickname}
              <button type="button" onClick={() => setReplyingTo(null)}>取消</button>
            </div>
          )}
          <input type="text" value={commentName} onChange={(e) => setCommentName(e.target.value)} placeholder="你的昵称" aria-label="评论昵称" />
          <textarea value={commentContent} onChange={(e) => setCommentContent(e.target.value)} placeholder="写下评论..." aria-label="评论内容" rows={4} />
          <button type="submit"><SendIcon size={14} /> 发布评论</button>
        </form>

        <div className="guestbook-comments">
          {comments.map((comment) => (
            <GuestbookComment key={comment.id} comment={comment} replyingTo={replyingTo?.id ?? null} onReply={(item) => setReplyingTo((current) => current?.id === item.id ? null : item)} />
          ))}
        </div>

        {comments.length === 0 && (
          <div className="py-16 text-center">
            <p style={{ color: "var(--muted-ink)" }}>{commentsLoading || loading ? "正在翻阅留言..." : "留言区暂时还是空白"}</p>
          </div>
        )}
      </section>
    </PageLayout>
  );
}
