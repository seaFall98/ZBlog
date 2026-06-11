import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRightIcon, CalendarIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { selectFeaturedPosts } from "../features/blog/homeSelectors";
import { usePosts } from "../features/blog/usePosts";
import { useAlbums } from "../features/gallery/useAlbums";
import { useMoments } from "../features/moments/useMoments";
import { useSiteProfile } from "../features/site/useSiteProfile";
import { useSiteStats } from "../features/stats/useSiteStats";
import { toDateText } from "../lib/text";

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
  }, [duration, target, trigger]);

  return count;
}

function StatItem({
  value,
  label,
  trigger,
}: {
  value: number;
  label: string;
  trigger: boolean;
}) {
  const count = useCountUp(value, trigger);

  return (
    <div className="flex flex-col items-center gap-1">
      <span
        className="block"
        style={{
          fontFamily: "var(--fontDisplay)",
          fontSize: "clamp(40px,5vw,72px)",
          fontWeight: 700,
          color: "var(--ink)",
          lineHeight: 1,
        }}
      >
        {count.toLocaleString()}
      </span>
      <span
        className="text-xs uppercase tracking-widest"
        style={{ color: "var(--muted-ink)" }}
      >
        {label}
      </span>
    </div>
  );
}

export default function Index() {
  const statsRef = useRef<HTMLDivElement>(null);
  const [statsVisible, setStatsVisible] = useState(false);
  const { profile } = useSiteProfile();
  const { posts } = usePosts({ pageSize: 50 });
  const { albums } = useAlbums(4);
  const { moments } = useMoments(3);
  const stats = useSiteStats();

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setStatsVisible(true);
        }
      },
      { threshold: 0.3 },
    );

    if (statsRef.current) {
      observer.observe(statsRef.current);
    }

    return () => observer.disconnect();
  }, []);

  const featuredPosts = selectFeaturedPosts(posts, 3);
  const latestPosts = posts.slice(0, 3);
  const galleryImages = albums.map((album) => album.coverUrl).filter(Boolean).slice(0, 4);
  const heroEyebrow = [profile.heroEyebrow, profile.established].filter(Boolean).join(" · ");
  const heroTitleLines = profile.heroTitle
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  const heroMeta = profile.heroMeta;

  return (
    <PageLayout>
      <section className="mx-auto flex max-w-7xl flex-wrap gap-12 px-8 pt-20 pb-16 md:flex-nowrap">
        <div className="flex min-w-64 flex-1 flex-col justify-between" style={{ flexBasis: "55%" }}>
          <div>
            {heroEyebrow && (
              <p
                className="mb-6 text-xs uppercase tracking-widest"
                style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
              >
                {heroEyebrow}
              </p>
            )}

            <h1
              className="mb-6 leading-tight"
              style={{
                fontFamily: "var(--fontDisplay)",
                fontSize: "clamp(40px,6vw,80px)",
                fontWeight: 400,
                color: "var(--ink)",
                lineHeight: 1.15,
              }}
            >
              {heroTitleLines.map((line, index) => (
                <span key={`${line}-${index}`}>
                  {index === heroTitleLines.length - 1 ? (
                    <em style={{ fontStyle: "italic", color: "var(--clay)" }}>{line}</em>
                  ) : (
                    line
                  )}
                  {index < heroTitleLines.length - 1 && <br />}
                </span>
              ))}
            </h1>

            {heroMeta && (
              <p className="mb-10 text-sm" style={{ color: "var(--muted-ink)" }}>
                {heroMeta}
              </p>
            )}
          </div>

          {profile.heroCtaLabel && profile.heroCtaTarget && (
            <Link
              to={profile.heroCtaTarget}
              className="group inline-flex items-center gap-2 text-sm"
              style={{ color: "var(--olive)", fontFamily: "var(--fontSans)" }}
            >
              {profile.heroCtaLabel}
              <ArrowRightIcon size={14} className="transition-transform group-hover:translate-x-1" />
            </Link>
          )}
        </div>

        <div
          className="flex min-w-56 flex-1 flex-col justify-center gap-0"
          style={{
            flexBasis: "42%",
            borderLeft: "1px solid var(--warm-border)",
            paddingLeft: "40px",
          }}
        >
          <div
            className="mb-5 text-xs uppercase tracking-widest"
            style={{ color: "var(--muted-ink)" }}
          >
            精选
          </div>

          {featuredPosts.map((post, index) => (
            <Link
              key={post.id}
              to={`/posts/${post.slug}`}
              className="group block border-b py-5 transition-opacity hover:opacity-70"
              style={{ borderColor: "var(--warm-border)" }}
            >
              <div className="flex items-start gap-3">
                <span
                  className="mt-1 min-w-[20px] shrink-0 text-xs"
                  style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                >
                  {String(index + 1).padStart(2, "0")}
                </span>
                <div>
                  {post.category?.name && (
                    <span
                      className="mb-1.5 inline-block rounded-sm px-2 py-0.5 text-xs"
                      style={{
                        background: "var(--section-bg)",
                        color: "var(--muted-ink)",
                        fontFamily: "var(--fontSans)",
                      }}
                    >
                      {post.category.name}
                    </span>
                  )}
                  <h3
                    className="leading-snug"
                    style={{
                      fontFamily: "var(--fontDisplay)",
                      fontSize: "15px",
                      color: "var(--ink)",
                      fontWeight: 500,
                    }}
                  >
                    {post.title}
                  </h3>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-8">
        <div style={{ height: "1px", background: "var(--warm-border)" }} />
      </div>

      <section className="mx-auto max-w-7xl px-8 py-20">
        <div className="mb-10 flex items-baseline justify-between">
          <h2
            style={{
              fontFamily: "var(--fontDisplay)",
              fontSize: "28px",
              fontWeight: 400,
              color: "var(--ink)",
            }}
          >
            近期文章
          </h2>
          <Link
            to="/blog"
            className="text-sm transition-opacity hover:opacity-60"
            style={{ color: "var(--muted-ink)" }}
          >
            全部 →
          </Link>
        </div>

        <div className="flex flex-wrap gap-8 md:flex-nowrap">
          {latestPosts.map((post, index) => (
            <Link
              key={post.id}
              to={`/posts/${post.slug}`}
              className="group block min-w-60 flex-1 transition-transform duration-300 hover:-translate-y-1"
            >
              {post.coverUrl && (
                <div className="mb-4 overflow-hidden" style={{ height: index === 1 ? "280px" : "220px" }}>
                  <img
                    src={post.coverUrl}
                    alt={post.title}
                    loading="lazy"
                    className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-103"
                    style={{ transition: "transform 0.5s ease" }}
                  />
                </div>
              )}

              <div className="mb-2 flex items-center gap-3">
                {post.category?.name && (
                  <span
                    className="text-xs"
                    style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                  >
                    {post.category.name}
                  </span>
                )}
                <span className="text-xs" style={{ color: "var(--warm-border)" }}>
                  ·
                </span>
                <span
                  className="flex items-center gap-1 text-xs"
                  style={{ color: "var(--muted-ink)" }}
                >
                  <CalendarIcon size={11} />
                  {toDateText(post.publishedAt)}
                </span>
              </div>

              <h3
                className="leading-snug"
                style={{
                  fontFamily: "var(--fontDisplay)",
                  fontSize: "17px",
                  fontWeight: 500,
                  color: "var(--ink)",
                }}
              >
                {post.title}
              </h3>
            </Link>
          ))}
        </div>
      </section>

      {galleryImages.length > 0 && (
        <section className="overflow-hidden" style={{ background: "var(--ink)" }}>
          <div className="mx-auto max-w-7xl px-8 py-12">
            <div className="mb-6 flex items-center justify-between">
              <h2
                style={{
                  fontFamily: "var(--fontDisplay)",
                  fontSize: "22px",
                  fontWeight: 400,
                  color: "var(--warm-white)",
                }}
              >
                近期影像
              </h2>
              <Link
                to="/gallery"
                className="text-sm transition-opacity hover:opacity-60"
                style={{ color: "rgba(255,255,255,0.5)" }}
              >
                查看相册 →
              </Link>
            </div>

            <div className="flex h-72 gap-0.5">
              {galleryImages.map((source, index) => {
                const widths = ["30%", "25%", "22%", "23%"];
                return (
                  <Link
                    key={source}
                    to="/gallery"
                    className="group cursor-pointer overflow-hidden"
                    style={{ width: widths[index] ?? "25%", flexShrink: 0 }}
                  >
                    <img
                      src={source}
                      alt=""
                      loading="lazy"
                      className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                    />
                  </Link>
                );
              })}
            </div>
          </div>
        </section>
      )}

      {moments.length > 0 && (
        <section className="mx-auto max-w-7xl px-8 py-20">
          <div className="mb-10 flex items-baseline justify-between">
            <h2
              style={{
                fontFamily: "var(--fontDisplay)",
                fontSize: "28px",
                fontWeight: 400,
                color: "var(--ink)",
              }}
            >
              生活瞬间
            </h2>
            <Link
              to="/moments"
              className="text-sm transition-opacity hover:opacity-60"
              style={{ color: "var(--muted-ink)" }}
            >
              更多瞬间 →
            </Link>
          </div>

          <div className="flex flex-wrap gap-6 md:flex-nowrap">
            <Link
              to="/moments"
              className="group block min-w-60 flex-1 p-8 transition-transform duration-300 hover:-translate-y-1"
              style={{
                background: "var(--warm-white)",
                border: "1px solid var(--warm-border)",
              }}
            >
              <div
                className="mb-4 text-xs"
                style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
              >
                {toDateText(moments[0].date).replace(/-/g, "·")} · {moments[0].mood}
              </div>
              <p
                className="leading-relaxed"
                style={{
                  fontFamily: "var(--fontBody)",
                  fontSize: "15px",
                  color: "var(--ink)",
                  lineHeight: 1.9,
                }}
              >
                {moments[0].text}
              </p>
              {moments[0].images[0] && (
                <div className="mt-4 h-32 overflow-hidden">
                  <img
                    src={moments[0].images[0]}
                    alt=""
                    className="h-full w-full object-cover"
                  />
                </div>
              )}
            </Link>

            <div className="flex flex-col gap-4" style={{ flexBasis: "40%", minWidth: "220px" }}>
              {moments.slice(1, 3).map((moment, index) => (
                <Link
                  key={moment.id}
                  to="/moments"
                  className="group block p-6 transition-transform duration-300 hover:-translate-y-0.5"
                  style={{
                    background: index === 0 ? "var(--warm-white)" : "var(--section-bg)",
                    border: "1px solid var(--warm-border)",
                  }}
                >
                  <div className="mb-2 text-xs" style={{ color: "var(--muted-ink)" }}>
                    {toDateText(moment.date).replace(/-/g, "·")} · {moment.mood}
                  </div>
                  <p
                    className="line-clamp-2 text-sm leading-relaxed"
                    style={{ fontFamily: "var(--fontBody)", color: "var(--ink)" }}
                  >
                    {moment.text}
                  </p>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      <section
        ref={statsRef}
        style={{
          background: "var(--section-bg)",
          borderTop: "1px solid var(--warm-border)",
          borderBottom: "1px solid var(--warm-border)",
        }}
      >
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-around gap-12 px-8 py-16">
          <StatItem value={stats.totalArticles} label="篇文章" trigger={statsVisible} />
          <div
            style={{ width: "1px", height: "60px", background: "var(--warm-border)" }}
            className="hidden md:block"
          />
          <StatItem value={stats.totalVisits} label="次访问" trigger={statsVisible} />
          <div
            style={{ width: "1px", height: "60px", background: "var(--warm-border)" }}
            className="hidden md:block"
          />
          <StatItem value={stats.totalPhotos} label="张照片" trigger={statsVisible} />
          <div
            style={{ width: "1px", height: "60px", background: "var(--warm-border)" }}
            className="hidden md:block"
          />
          <StatItem value={stats.totalMessages} label="条留言" trigger={statsVisible} />
        </div>
      </section>
    </PageLayout>
  );
}
