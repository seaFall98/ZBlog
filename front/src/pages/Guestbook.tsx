import { useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { SendIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { AppPagination } from "../components/ui/app-pagination";
import CommentSection from "../features/comments/CommentSection";
import { fetchGuestbookMessages, submitGuestbookMessage } from "../features/guestbook/guestbookApi";
import { useGuestbookMessages } from "../features/guestbook/useGuestbookMessages";
import { useSiteProfile } from "../features/site/useSiteProfile";
import { useNormalizePage, usePage } from "../hooks/usePage";
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

export default function Guestbook() {
  const [content, setContent] = useState("");
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
          <span>留言与讨论</span>
        </div>
        <CommentSection targetType={COMMENT_TARGET_TYPE} targetKey={COMMENT_TARGET_KEY} compact />
      </section>
    </PageLayout>
  );
}
