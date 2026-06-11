import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { SendIcon } from "lucide-react";
import { toast } from "sonner";
import PageLayout from "../components/layout/PageLayout";
import { fetchComments, submitComment } from "../features/comments/commentApi";
import type { CommentView } from "../features/comments/types";
import { submitGuestbookMessage } from "../features/guestbook/guestbookApi";
import { useGuestbookMessages } from "../features/guestbook/useGuestbookMessages";
import { useSiteProfile } from "../features/site/useSiteProfile";
import { toDateText } from "../lib/text";

interface Danmaku {
  id: number;
  text: string;
  top: number;
  speed: number;
  delay: number;
  color: string;
}

const DANMAKU_COLORS = [
  "rgba(255,255,255,0.9)",
  "rgba(245,238,224,0.82)",
  "rgba(201,174,134,0.78)",
  "rgba(255,255,255,0.66)",
];
const COMMENT_TARGET_TYPE = "page";
const COMMENT_TARGET_KEY = "guestbook";
const DEFAULT_GUESTBOOK_NAME = "访客";

type GuestbookCommentProps = {
  comment: CommentView;
  depth?: number;
  replyingTo: string | null;
  onReply: (comment: CommentView) => void;
};

function GuestbookComment({
  comment,
  depth = 0,
  replyingTo,
  onReply,
}: GuestbookCommentProps) {
  return (
    <article
      className={`guestbook-comment ${depth > 0 ? "guestbook-comment--reply" : ""}`}
      id={`comment-${comment.id}`}
    >
      <img
        className="guestbook-comment__avatar"
        src={comment.avatar}
        alt={comment.nickname}
        loading="lazy"
      />
      <div className="guestbook-comment__body">
        <header className="guestbook-comment__header">
          <strong>{comment.nickname}</strong>
          {comment.createdAt && <span>{toDateText(comment.createdAt)}</span>}
        </header>
        <p>{comment.content}</p>
        <button type="button" onClick={() => onReply(comment)}>
          {replyingTo === comment.id ? "取消回复" : "回复"}
        </button>
        {comment.replies.length > 0 && (
          <div className="guestbook-comment__replies">
            {comment.replies.map((reply) => (
              <GuestbookComment
                key={reply.id}
                comment={reply}
                depth={depth + 1}
                replyingTo={replyingTo}
                onReply={onReply}
              />
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
  const [comments, setComments] = useState<CommentView[]>([]);
  const [commentsLoading, setCommentsLoading] = useState(true);
  const { messages, loading, reload } = useGuestbookMessages(50);
  const { profile } = useSiteProfile();
  const [submittedDanmakus, setSubmittedDanmakus] = useState<Danmaku[]>([]);
  const danmakuIdRef = useRef(1000);
  const backgroundImage = profile.barrageBackgroundImage || profile.backgroundImage;

  const danmakus = useMemo<Danmaku[]>(
    () => [
      ...messages.slice(0, 14).map((message, index) => ({
        id: index,
        text: `${message.name}：${message.content.slice(0, 34)}`,
        top: ((index * 11) % 64) + 12,
        speed: 20 + (index % 5) * 3,
        delay: -((index * 2.7) % 18),
        color:
          DANMAKU_COLORS[index % DANMAKU_COLORS.length] ??
          "rgba(255,255,255,0.78)",
      })),
      ...submittedDanmakus,
    ],
    [messages, submittedDanmakus],
  );

  const loadComments = useCallback(async () => {
    setCommentsLoading(true);
    try {
      setComments(await fetchComments(COMMENT_TARGET_TYPE, COMMENT_TARGET_KEY, 50));
    } catch {
      setComments([]);
    } finally {
      setCommentsLoading(false);
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      void loadComments();
    });
  }, [loadComments]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!content.trim()) {
      toast.error("请填写留言内容");
      return;
    }

    try {
      const result = await submitGuestbookMessage({
        nickname: DEFAULT_GUESTBOOK_NAME,
        content: content.trim(),
      });
      const newDanmaku: Danmaku = {
        id: ++danmakuIdRef.current,
        text: `${DEFAULT_GUESTBOOK_NAME}：${content.slice(0, 34)}`,
        top: ((danmakuIdRef.current * 11) % 64) + 12,
        speed: 17 + (danmakuIdRef.current % 5) * 2,
        delay: 0,
        color:
          DANMAKU_COLORS[danmakuIdRef.current % DANMAKU_COLORS.length] ??
          "rgba(255,255,255,0.78)",
      };
      setSubmittedDanmakus((previous) => [...previous, newDanmaku]);
      setContent("");
      toast.success(
        result.message ||
          (result.status === "pending" ? "留言已提交，等待审核" : "留言成功"),
      );
      void reload();
    } catch {
      toast.error("留言发送失败，请稍后再试");
    }
  };

  const handleCommentSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
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
      void loadComments();
    } catch {
      toast.error("评论发送失败，请稍后再试");
    }
  };

  const heroStyle = backgroundImage
    ? {
        backgroundImage: `url(${backgroundImage})`,
      }
    : {
        background:
          "linear-gradient(180deg, rgba(25, 25, 24, 0.92), rgba(40, 38, 34, 0.78))",
      };

  return (
    <PageLayout headerVariant="guestbook" noMainTopPadding>
      <section className="guestbook-hero" style={heroStyle}>
        <div className="pointer-events-none absolute inset-0 overflow-hidden">
          {danmakus.map((danmaku) => (
            <div
              key={danmaku.id}
              className="guestbook-danmaku"
              style={{
                top: `${danmaku.top}%`,
                color: danmaku.color,
                animationDuration: `${danmaku.speed}s`,
                animationDelay: `${danmaku.delay}s`,
              }}
            >
              {danmaku.text}
            </div>
          ))}
        </div>

        <div className="guestbook-hero__content">
          {profile.guestbookIntro && <p>{profile.guestbookIntro}</p>}
          <form onSubmit={handleSubmit} className="guestbook-form">
            <button type="submit">
              <SendIcon size={14} /> 发送
            </button>
            <input
              type="text"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="留下点什么啦"
              aria-label="留言内容"
            />
          </form>
        </div>
      </section>

      <section className="guestbook-board" aria-label="留言评论区">
        <div className="guestbook-board__heading">
          <h2>留言区</h2>
          <span>{commentsLoading ? "正在加载" : `${comments.length} 条评论`}</span>
        </div>

        <form className="guestbook-comment-form" onSubmit={handleCommentSubmit}>
          {replyingTo && (
            <div className="guestbook-comment-form__replying">
              正在回复 {replyingTo.nickname}
              <button type="button" onClick={() => setReplyingTo(null)}>
                取消
              </button>
            </div>
          )}
          <input
            type="text"
            value={commentName}
            onChange={(event) => setCommentName(event.target.value)}
            placeholder="你的昵称"
            aria-label="评论昵称"
          />
          <textarea
            value={commentContent}
            onChange={(event) => setCommentContent(event.target.value)}
            placeholder="写下评论..."
            aria-label="评论内容"
            rows={4}
          />
          <button type="submit">
            <SendIcon size={14} /> 发布评论
          </button>
        </form>

        <div className="guestbook-comments">
          {comments.map((comment) => (
            <GuestbookComment
              key={comment.id}
              comment={comment}
              replyingTo={replyingTo?.id ?? null}
              onReply={(item) =>
                setReplyingTo((current) => (current?.id === item.id ? null : item))
              }
            />
          ))}
        </div>

        {comments.length === 0 && (
          <div className="py-16 text-center">
            <p style={{ color: "var(--muted-ink)" }}>
              {commentsLoading || loading ? "正在翻阅留言..." : "留言区暂时还是空白"}
            </p>
          </div>
        )}
      </section>
    </PageLayout>
  );
}
