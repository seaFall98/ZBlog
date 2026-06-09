import { useEffect, useRef, useState } from "react";
import { SendIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { submitGuestbookMessage } from "../features/guestbook/guestbookApi";
import { useGuestbookMessages } from "../features/guestbook/useGuestbookMessages";
import { toDateText } from "../lib/text";
import { toast } from "sonner";

interface Danmaku {
  id: number;
  text: string;
  top: number;
  speed: number;
  color: string;
  left: number;
}

const DANMAKU_COLORS = ["rgba(255,255,255,0.86)", "rgba(245,238,224,0.72)", "rgba(201,174,134,0.72)", "rgba(255,255,255,0.52)"];

export default function Guestbook() {
  const [name, setName] = useState("");
  const [content, setContent] = useState("");
  const { messages, loading, reload } = useGuestbookMessages(50);
  const [danmakus, setDanmakus] = useState<Danmaku[]>([]);
  const danmakuIdRef = useRef(1000);

  useEffect(() => {
    const initial: Danmaku[] = messages.slice(0, 14).map((m, i) => ({
      id: i,
      text: `${m.name}：${m.content.slice(0, 34)}`,
      top: ((i * 11) % 68) + 8,
      speed: 18 + (i % 5) * 3,
      left: (i * 13) % 88,
      color: DANMAKU_COLORS[i % DANMAKU_COLORS.length] ?? "rgba(255,255,255,0.7)",
    }));
    setDanmakus(initial);
  }, [messages]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !content.trim()) {
      toast.error("请填写昵称和留言内容");
      return;
    }

    try {
      const result = await submitGuestbookMessage({ nickname: name.trim(), content: content.trim() });
      const newDanmaku: Danmaku = {
        id: ++danmakuIdRef.current,
        text: `${name}：${content.slice(0, 34)}`,
        top: ((danmakuIdRef.current * 11) % 68) + 8,
        speed: 14 + (danmakuIdRef.current % 5) * 2,
        left: 90,
        color: DANMAKU_COLORS[danmakuIdRef.current % DANMAKU_COLORS.length] ?? "rgba(255,255,255,0.7)",
      };
      setDanmakus((prev) => [...prev, newDanmaku]);
      setName("");
      setContent("");
      toast.success(result.message || (result.status === "pending" ? "留言已提交，等待审核" : "留言成功 ✨"));
      void reload();
    } catch {
      toast.error("留言发送失败，请稍后再试");
    }
  };

  return (
    <PageLayout>
      <section className="guestbook-hero">
        <div className="guestbook-hero__shade" />
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
          {danmakus.map((d) => (
            <div
              key={d.id}
              className="guestbook-danmaku-vertical"
              style={{ top: `${d.top}%`, left: `${d.left}%`, color: d.color, animationDuration: `${d.speed}s` }}
            >
              {d.text}
            </div>
          ))}
        </div>

        <div className="guestbook-hero__content">
          <p className="text-xs tracking-widest uppercase mb-4">Guestbook</p>
          <h1>留下你的话</h1>
          <p>你的留言会穿过这面安静的墙，也会真实保存到 ZBlog。</p>
          <form onSubmit={handleSubmit} className="guestbook-form">
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="你的昵称" />
            <input type="text" value={content} onChange={(e) => setContent(e.target.value)} placeholder="说点什么吧..." />
            <button type="submit"><SendIcon size={14} /> 发送</button>
          </form>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-8 py-20">
        <div className="flex items-baseline justify-between mb-10">
          <h2 style={{ fontFamily: "var(--fontDisplay)", fontSize: "26px", fontWeight: 400, color: "var(--ink)" }}>留言墙</h2>
          <span className="text-xs" style={{ color: "var(--muted-ink)" }}>{loading ? "正在加载" : `${messages.length} 条留言`}</span>
        </div>

        <div className="guestbook-message-wall">
          {messages.map((msg) => (
            <article key={msg.id} className="guestbook-vertical-message">
              <div className="guestbook-vertical-message__avatar"><img src={msg.avatar} alt={msg.name} /></div>
              <p>{msg.content}</p>
              <footer>{msg.name} · {toDateText(msg.date)}</footer>
            </article>
          ))}
        </div>

        {messages.length === 0 && (
          <div className="py-20 text-center">
            <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅留言..." : "留言墙暂时还是空白"}</p>
          </div>
        )}
      </section>
    </PageLayout>
  );
}
