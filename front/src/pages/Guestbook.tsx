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

const DANMAKU_COLORS = ["var(--olive)", "var(--clay)", "var(--slate-blue)", "var(--muted-ink)", "var(--ink)"];

export default function Guestbook() {
  const [name, setName] = useState("");
  const [content, setContent] = useState("");
  const { messages, loading, reload } = useGuestbookMessages(50);
  const [danmakus, setDanmakus] = useState<Danmaku[]>([]);
  const danmakuIdRef = useRef(1000);

  // Init danmaku from existing messages
  useEffect(() => {
    const initial: Danmaku[] = messages.slice(0, 12).map((m, i) => ({
      id: i,
      text: `${m.name}: ${m.content.slice(0, 30)}`,
      top: ((i * 17) % 70) + 5,
      speed: 18 + (i % 5) * 3,
      left: (i * 23) % 80,
      color: DANMAKU_COLORS[i % DANMAKU_COLORS.length] ?? "var(--ink)",
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
      await submitGuestbookMessage({ nickname: name.trim(), content: content.trim() });

      // Add danmaku
      const newDanmaku: Danmaku = {
        id: ++danmakuIdRef.current,
        text: `${name}: ${content.slice(0, 30)}`,
        top: ((danmakuIdRef.current * 17) % 70) + 5,
        speed: 14 + (danmakuIdRef.current % 5) * 2,
        left: 100,
        color: DANMAKU_COLORS[danmakuIdRef.current % DANMAKU_COLORS.length] ?? "var(--ink)",
      };
      setDanmakus((prev) => [...prev, newDanmaku]);

      setName("");
      setContent("");
      toast.success("留言成功 ✨");
      void reload();
    } catch {
      toast.error("留言发送失败，请稍后再试");
    }
  };

  return (
    <PageLayout>
      {/* ── Hero Section: full-width immersive ── */}
      <div
        className="relative w-full overflow-hidden"
        style={{ height: "70vh", minHeight: "500px", background: "var(--ink)" }}
      >
        {/* Leaf fall decoration */}
        <div className="leaf-container absolute inset-0 pointer-events-none overflow-hidden">
          {Array.from({ length: 12 }).map((_, i) => (
            <div
              key={i}
              className="absolute text-xl"
              style={{
                left: `${(i * 8.3) % 100}%`,
                animationName: "leafFall",
                animationDuration: `${4 + (i % 4) * 1.5}s`,
                animationDelay: `${(i * 0.8) % 5}s`,
                animationTimingFunction: "linear",
                animationIterationCount: "infinite",
                opacity: 0.12,
                fontSize: `${14 + (i % 3) * 6}px`,
              }}
            >
              {["🍂", "🍁", "🌿", "✦", "·"][i % 5]}
            </div>
          ))}
        </div>

        {/* Danmaku layer */}
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
          {danmakus.map((d) => (
            <div
              key={d.id}
              className="absolute whitespace-nowrap text-sm danmaku-item"
              style={{
                top: `${d.top}%`,
                left: `${d.left}%`,
                color: d.color,
                fontFamily: "var(--fontSans)",
                opacity: 0.55,
                fontSize: "13px",
                animation: `marqueeScroll ${d.speed}s linear infinite`,
              }}
            >
              {d.text}
            </div>
          ))}
        </div>

        {/* Center input */}
        <div className="absolute inset-0 flex flex-col items-center justify-center z-10 px-8">
          <h1
            className="mb-3 text-center"
            style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(32px,4vw,52px)", fontWeight: 400, color: "rgba(255,255,255,0.92)" }}
          >
            留下你的话
          </h1>
          <p
            className="mb-10 text-center text-sm"
            style={{ color: "rgba(255,255,255,0.4)", fontFamily: "var(--fontSans)" }}
          >
            你的留言会以弹幕飘过
          </p>

          <form onSubmit={handleSubmit} className="w-full max-w-lg">
            <div className="flex gap-3 mb-3">
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="你的昵称"
                className="flex-1 h-10 px-4 text-sm bg-transparent border-b outline-none"
                style={{
                  borderColor: "rgba(255,255,255,0.2)",
                  color: "rgba(255,255,255,0.85)",
                  fontFamily: "var(--fontSans)",
                }}
              />
            </div>
            <div className="flex gap-3">
              <input
                type="text"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="说点什么吧..."
                className="flex-1 h-12 px-4 text-sm bg-transparent border-b outline-none"
                style={{
                  borderColor: "rgba(255,255,255,0.2)",
                  color: "rgba(255,255,255,0.85)",
                  fontFamily: "var(--fontSans)",
                }}
              />
              <button
                type="submit"
                className="shrink-0 px-5 h-12 flex items-center gap-2 text-sm transition-opacity hover:opacity-80"
                style={{
                  background: "rgba(255,255,255,0.12)",
                  border: "1px solid rgba(255,255,255,0.2)",
                  color: "rgba(255,255,255,0.85)",
                  fontFamily: "var(--fontSans)",
                }}
              >
                <SendIcon size={14} /> 发送
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* ── Formal Message List ── */}
      <div className="max-w-5xl mx-auto px-8 py-20">
        <div className="flex items-baseline justify-between mb-10">
          <h2 style={{ fontFamily: "var(--fontDisplay)", fontSize: "26px", fontWeight: 400, color: "var(--ink)" }}>
            留言墙
          </h2>
          <span className="text-xs" style={{ color: "var(--muted-ink)" }}>{loading ? "正在加载" : `${messages.length} 条留言`}</span>
        </div>

        <div className="flex flex-wrap gap-5">
          {messages.map((msg) => (
            <div
              key={msg.id}
              className="p-6"
              style={{
                background: "var(--warm-white)",
                border: "1px solid var(--warm-border)",
                flexBasis: "calc(50% - 10px)",
                minWidth: "260px",
              }}
            >
              <div className="flex items-center gap-3 mb-4">
                <img
                  src={msg.avatar}
                  alt={msg.name}
                  className="w-9 h-9 rounded-full"
                  style={{ background: "var(--section-bg)" }}
                />
                <div>
                  <div
                    className="text-sm font-medium"
                    style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}
                  >
                    {msg.name}
                  </div>
                  <div className="text-xs" style={{ color: "var(--muted-ink)" }}>{toDateText(msg.date)}</div>
                </div>
              </div>
              <p
                className="leading-relaxed text-sm"
                style={{ fontFamily: "var(--fontBody)", color: "var(--ink)", lineHeight: 1.85 }}
              >
                {msg.content}
              </p>
            </div>
          ))}
        </div>

        {messages.length === 0 && (
          <div className="py-20 text-center">
            <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅留言..." : "留言墙暂时还是空白"}</p>
          </div>
        )}
      </div>
    </PageLayout>
  );
}
