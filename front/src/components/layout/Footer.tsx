import { useState } from "react";
import { Link } from "react-router-dom";
import { SendIcon } from "lucide-react";

export default function Footer() {
  const [email, setEmail] = useState("");
  const [subscribed, setSubscribed] = useState(false);

  const handleSubscribe = (e: React.FormEvent) => {
    e.preventDefault();
    if (email.trim()) {
      setSubscribed(true);
    }
  };

  return (
    <footer
      data-cmp="Footer"
      className="mt-24 border-t"
      style={{ background: "var(--section-bg)", borderColor: "var(--warm-border)" }}
    >
      <div className="max-w-7xl mx-auto px-8 py-16">
        <div className="flex flex-wrap gap-12 justify-between">
          {/* Left: Logo + intro */}
          <div className="flex-1 min-w-48 max-w-64">
            <div style={{ fontFamily: "var(--fontDisplay)", fontSize: "20px", color: "var(--ink)", marginBottom: "12px" }}>
              寂静之书
            </div>
            <p className="text-sm leading-relaxed" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
              记录平凡生活里的光与影，写作是一种安静的对话。
            </p>
          </div>

          {/* Center: Nav groups */}
          <div className="flex gap-16 flex-wrap">
            <div>
              <div className="text-xs font-medium mb-4 tracking-widest uppercase" style={{ color: "var(--muted-ink)" }}>写作</div>
              <div className="flex flex-col gap-2.5">
                <Link to="/blog" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>文章列表</Link>
                <Link to="/categories" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>分类</Link>
                <Link to="/tags" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>标签</Link>
                <Link to="/archive" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>归档</Link>
              </div>
            </div>
            <div>
              <div className="text-xs font-medium mb-4 tracking-widest uppercase" style={{ color: "var(--muted-ink)" }}>影像</div>
              <div className="flex flex-col gap-2.5">
                <Link to="/gallery" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>相册图库</Link>
                <Link to="/moments" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>生活瞬间</Link>
              </div>
            </div>
            <div>
              <div className="text-xs font-medium mb-4 tracking-widest uppercase" style={{ color: "var(--muted-ink)" }}>社交</div>
              <div className="flex flex-col gap-2.5">
                <Link to="/guestbook" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>留言墙</Link>
                <Link to="/links" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>友情链接</Link>
                <Link to="/about" className="text-sm hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>关于博主</Link>
              </div>
            </div>
          </div>

          {/* Right: Subscribe */}
          <div className="min-w-52">
            <div className="text-xs font-medium mb-4 tracking-widest uppercase" style={{ color: "var(--muted-ink)" }}>订阅更新</div>
            {subscribed ? (
              <div className="flex items-center gap-2 text-sm" style={{ color: "var(--olive)" }}>
                <span>✓</span>
                <span>感谢订阅</span>
              </div>
            ) : (
              <form onSubmit={handleSubscribe} className="flex gap-2">
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="你的邮箱"
                  className="flex-1 h-9 text-sm bg-transparent border-b outline-none px-1"
                  style={{ borderColor: "var(--warm-border)", fontFamily: "var(--fontSans)", color: "var(--ink)", minWidth: 0 }}
                />
                <button
                  type="submit"
                  className="shrink-0 w-9 h-9 flex items-center justify-center rounded-sm transition-colors hover:opacity-80"
                  style={{ background: "var(--ink)", color: "var(--warm-white)" }}
                >
                  <SendIcon size={14} />
                </button>
              </form>
            )}
          </div>
        </div>

        <div className="mt-12 pt-6 border-t flex flex-wrap justify-between items-center gap-4" style={{ borderColor: "var(--warm-border)" }}>
          <p className="text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
            © 2024 寂静之书 · 以文字作舟，渡光阴之河
          </p>
          <div className="flex gap-6">
            <Link to="/stats" className="text-xs hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>站点统计</Link>
            <Link to="/guestbook" className="text-xs hover:text-primary transition-colors" style={{ color: "var(--muted-ink)" }}>留言</Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
