import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRightIcon, CalendarIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { selectFeaturedPosts } from "../features/blog/homeSelectors";
import { usePosts } from "../features/blog/usePosts";
import { useAlbums } from "../features/gallery/useAlbums";
import { useMoments } from "../features/moments/useMoments";
import { useSiteStats } from "../features/stats/useSiteStats";
import { toDateText } from "../lib/text";

// Number counter hook
function useCountUp(target: number, trigger: boolean, duration = 1200) {
  const [count, setCount] = useState(0);
  useEffect(() => {
    if (!trigger) return;
    let start = 0;
    const step = target / (duration / 16);
    const timer = setInterval(() => {
      start += step;
      if (start >= target) {
        setCount(target);
        clearInterval(timer);
      } else {
        setCount(Math.floor(start));
      }
    }, 16);
    return () => clearInterval(timer);
  }, [trigger, target, duration]);
  return count;
}

function StatItem({ value, label, trigger }: { value: number; label: string; trigger: boolean }) {
  const count = useCountUp(value, trigger);
  return (
    <div className="flex flex-col items-center gap-1">
      <span
        className="block"
        style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(40px,5vw,72px)", fontWeight: 700, color: "var(--ink)", lineHeight: 1 }}
      >
        {count.toLocaleString()}
      </span>
      <span className="text-xs tracking-widest uppercase" style={{ color: "var(--muted-ink)" }}>{label}</span>
    </div>
  );
}

export default function Index() {
  const statsRef = useRef<HTMLDivElement>(null);
  const [statsVisible, setStatsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setStatsVisible(true); },
      { threshold: 0.3 }
    );
    if (statsRef.current) observer.observe(statsRef.current);
    return () => observer.disconnect();
  }, []);

  const { posts } = usePosts({ pageSize: 50 });
  const { albums } = useAlbums(4);
  const { moments } = useMoments(3);
  const stats = useSiteStats();
  const featuredPosts = selectFeaturedPosts(posts, 3);
  const latestPosts = posts.slice(0, 3);
  const galleryImages = albums.map((album) => album.coverUrl).filter(Boolean).slice(0, 4);

  return (
    <PageLayout>
      {/* ── Hero ─────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-8 pt-20 pb-16 flex gap-12 flex-wrap md:flex-nowrap">
        {/* Left 55% */}
        <div className="flex-1 min-w-64 flex flex-col justify-between" style={{ flexBasis: "55%" }}>
          <div>
            <p className="text-xs tracking-widest uppercase mb-6" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
              个人出版物 · 2024
            </p>
            <h1
              className="leading-tight mb-6"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(40px,6vw,80px)", fontWeight: 400, color: "var(--ink)", lineHeight: 1.15 }}
            >
              以文字作舟，<br />
              渡光阴<br />
              <em style={{ fontStyle: "italic", color: "var(--clay)" }}>之河</em>
            </h1>
            <p className="text-sm mb-3" style={{ color: "var(--muted-ink)" }}>
              {(stats.totalArticles || posts.length).toLocaleString()} 篇文章 · 最近更新于 {latestPosts[0] ? toDateText(latestPosts[0].publishedAt) : "未发布"}
            </p>
          </div>
          <Link
            to="/blog"
            className="inline-flex items-center gap-2 text-sm group"
            style={{ color: "var(--olive)", fontFamily: "var(--fontSans)" }}
          >
            阅读文章
            <ArrowRightIcon size={14} className="transition-transform group-hover:translate-x-1" />
          </Link>
        </div>

        {/* Right 45%: featured list */}
        <div
          className="flex-1 min-w-56 flex flex-col justify-center gap-0"
          style={{ flexBasis: "42%", borderLeft: "1px solid var(--warm-border)", paddingLeft: "40px" }}
        >
          <div className="text-xs tracking-widest uppercase mb-5" style={{ color: "var(--muted-ink)" }}>精选</div>
          {featuredPosts.map((post, idx) => (
            <Link
              key={post.id}
              to={`/posts/${post.slug}`}
              className="group block py-5 border-b hover:opacity-70 transition-opacity"
              style={{ borderColor: "var(--warm-border)" }}
            >
              <div className="flex items-start gap-3">
                <span
                  className="shrink-0 text-xs mt-1"
                  style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)", minWidth: "20px" }}
                >
                  0{idx + 1}
                </span>
                <div>
                  <span
                    className="inline-block text-xs mb-1.5 px-2 py-0.5 rounded-sm"
                    style={{ background: "var(--section-bg)", color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                  >
                    {post.category?.name ?? "未分类"}
                  </span>
                  <h3
                    className="leading-snug"
                    style={{ fontFamily: "var(--fontDisplay)", fontSize: "15px", color: "var(--ink)", fontWeight: 500 }}
                  >
                    {post.title}
                  </h3>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* ── Divider ── */}
      <div className="max-w-7xl mx-auto px-8">
        <div style={{ height: "1px", background: "var(--warm-border)" }} />
      </div>

      {/* ── Recent Posts ─────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-8 py-20">
        <div className="flex items-baseline justify-between mb-10">
          <h2 style={{ fontFamily: "var(--fontDisplay)", fontSize: "28px", fontWeight: 400, color: "var(--ink)" }}>近期文章</h2>
          <Link to="/blog" className="text-sm hover:opacity-60 transition-opacity" style={{ color: "var(--muted-ink)" }}>
            全部 →
          </Link>
        </div>
        <div className="flex gap-8 flex-wrap md:flex-nowrap">
          {latestPosts.map((post, idx) => (
            <Link
              key={post.id}
              to={`/posts/${post.slug}`}
              className="group flex-1 min-w-60 block hover:-translate-y-1 transition-transform duration-300"
            >
              {post.coverUrl && (
                <div
                  className="overflow-hidden mb-4"
                  style={{ height: idx === 1 ? "280px" : "220px" }}
                >
                  <img
                    src={post.coverUrl}
                    alt={post.title}
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-103 transition-transform duration-500"
                    style={{ transition: "transform 0.5s ease" }}
                  />
                </div>
              )}
              <div className="flex items-center gap-3 mb-2">
                <span className="text-xs" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                  {post.category?.name ?? "未分类"}
                </span>
                <span className="text-xs" style={{ color: "var(--warm-border)" }}>·</span>
                <span className="text-xs flex items-center gap-1" style={{ color: "var(--muted-ink)" }}>
                  <CalendarIcon size={11} />{toDateText(post.publishedAt)}
                </span>
              </div>
              <h3
                className="leading-snug"
                style={{ fontFamily: "var(--fontDisplay)", fontSize: "17px", fontWeight: 500, color: "var(--ink)" }}
              >
                {post.title}
              </h3>
            </Link>
          ))}
        </div>
      </section>

      {/* ── Gallery strip ────────────────────────────── */}
      {galleryImages.length > 0 && (
        <section style={{ background: "var(--ink)" }} className="overflow-hidden">
          <div className="max-w-7xl mx-auto px-8 py-12">
            <div className="flex items-center justify-between mb-6">
              <h2 style={{ fontFamily: "var(--fontDisplay)", fontSize: "22px", fontWeight: 400, color: "var(--warm-white)" }}>
                近期影像
              </h2>
              <Link to="/gallery" className="text-sm transition-opacity hover:opacity-60" style={{ color: "rgba(255,255,255,0.5)" }}>
                查看相册 →
              </Link>
            </div>
            <div className="flex gap-0.5 h-72">
              {galleryImages.map((src, idx) => {
                const widths = ["30%", "25%", "22%", "23%"];
                return (
                  <Link
                    key={src}
                    to="/gallery"
                    className="overflow-hidden group cursor-pointer"
                    style={{ width: widths[idx] ?? "25%", flexShrink: 0 }}
                  >
                    <img
                      src={src}
                      alt=""
                      loading="lazy"
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  </Link>
                );
              })}
            </div>
          </div>
        </section>
      )}

      {/* ── Moments preview ──────────────────────────── */}
      {moments.length > 0 && (
        <section className="max-w-7xl mx-auto px-8 py-20">
          <div className="flex items-baseline justify-between mb-10">
            <h2 style={{ fontFamily: "var(--fontDisplay)", fontSize: "28px", fontWeight: 400, color: "var(--ink)" }}>生活瞬间</h2>
            <Link to="/moments" className="text-sm hover:opacity-60 transition-opacity" style={{ color: "var(--muted-ink)" }}>
              更多瞬间 →
            </Link>
          </div>
          <div className="flex gap-6 flex-wrap md:flex-nowrap">
            <Link
              to="/moments"
              className="group flex-1 min-w-60 block p-8 hover:-translate-y-1 transition-transform duration-300"
              style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)" }}
            >
              <div className="text-xs mb-4" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                {toDateText(moments[0].date).replace(/-/g, "·")} · {moments[0].mood}
              </div>
              <p
                className="leading-relaxed"
                style={{ fontFamily: "var(--fontBody)", fontSize: "15px", color: "var(--ink)", lineHeight: 1.9 }}
              >
                {moments[0].text}
              </p>
              {moments[0].images[0] && (
                <div className="mt-4 h-32 overflow-hidden">
                  <img src={moments[0].images[0]} alt="" className="w-full h-full object-cover" />
                </div>
              )}
            </Link>

            <div className="flex flex-col gap-4" style={{ flexBasis: "40%", minWidth: "220px" }}>
              {moments.slice(1, 3).map((moment, index) => (
                <Link
                  key={moment.id}
                  to="/moments"
                  className="group block p-6 hover:-translate-y-0.5 transition-transform duration-300"
                  style={{ background: index === 0 ? "var(--warm-white)" : "var(--section-bg)", border: "1px solid var(--warm-border)" }}
                >
                  <div className="text-xs mb-2" style={{ color: "var(--muted-ink)" }}>{toDateText(moment.date).replace(/-/g, "·")} · {moment.mood}</div>
                  <p className="text-sm leading-relaxed line-clamp-2" style={{ fontFamily: "var(--fontBody)", color: "var(--ink)" }}>
                    {moment.text}
                  </p>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ── Stats strip ──────────────────────────────── */}
      <section
        ref={statsRef}
        style={{ background: "var(--section-bg)", borderTop: "1px solid var(--warm-border)", borderBottom: "1px solid var(--warm-border)" }}
      >
        <div className="max-w-7xl mx-auto px-8 py-16 flex flex-wrap gap-12 justify-around items-center">
          <StatItem value={stats.totalArticles} label="篇文章" trigger={statsVisible} />
          <div style={{ width: "1px", height: "60px", background: "var(--warm-border)" }} className="hidden md:block" />
          <StatItem value={stats.totalVisits} label="次访问" trigger={statsVisible} />
          <div style={{ width: "1px", height: "60px", background: "var(--warm-border)" }} className="hidden md:block" />
          <StatItem value={stats.totalPhotos} label="张照片" trigger={statsVisible} />
          <div style={{ width: "1px", height: "60px", background: "var(--warm-border)" }} className="hidden md:block" />
          <StatItem value={stats.totalMessages} label="条留言" trigger={statsVisible} />
        </div>
      </section>
    </PageLayout>
  );
}
