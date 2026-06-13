import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ChevronDownIcon, ChevronUpIcon, CalendarIcon, ClockIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { AppPagination } from "../components/ui/app-pagination";
import type { ArchiveYear } from "../features/blog/archive";
import { usePosts } from "../features/blog/usePosts";
import { useArchiveStats } from "../features/stats/useArchiveStats";
import { useNormalizePage, usePage } from "../hooks/usePage";
import { toDateText } from "../lib/text";

const MONTH_PAGE_SIZE = 12;
const POST_PAGE_SIZE = 12;

function paginateArchiveMonths(archiveYears: ArchiveYear[], page: number): ArchiveYear[] {
  const months = archiveYears.flatMap((group) => group.months.map((month) => ({ year: group.year, month })));
  const start = (page - 1) * MONTH_PAGE_SIZE;
  const pageMonths = months.slice(start, start + MONTH_PAGE_SIZE);
  const grouped = new Map<number, ArchiveYear>();

  pageMonths.forEach(({ year, month }) => {
    const current = grouped.get(year) ?? { year, count: 0, months: [] };
    current.months.push(month);
    current.count += month.count;
    grouped.set(year, current);
  });

  return Array.from(grouped.values());
}

export default function Archive() {
  const { year, month } = useParams();
  const isMonthArchive = Boolean(year && month);
  const { page, setPage } = usePage();
  const { posts, total, loading: postsLoading } = usePosts(
    { year, month, page, pageSize: POST_PAGE_SIZE, enabled: isMonthArchive },
  );
  const { archiveYears, loading: archiveLoading } = useArchiveStats();
  const archiveMonthCount = archiveYears.reduce((count, group) => count + group.months.length, 0);
  const archiveTotalPages = Math.ceil(archiveMonthCount / MONTH_PAGE_SIZE);
  const postTotalPages = Math.ceil(total / POST_PAGE_SIZE);
  const displayedArchiveYears = useMemo(() => paginateArchiveMonths(archiveYears, page), [archiveYears, page]);
  const defaultOpenYears = useMemo(() => {
    const next: Record<string, boolean> = {};
    displayedArchiveYears.forEach((group, index) => {
      next[String(group.year)] = index === 0;
    });
    return next;
  }, [displayedArchiveYears]);
  const [openYears, setOpenYears] = useState<Record<string, boolean>>({});
  const loading = isMonthArchive ? postsLoading : archiveLoading;
  useNormalizePage(page, setPage, isMonthArchive ? postTotalPages : archiveTotalPages, loading);
  const archiveArticleCount = archiveYears.reduce((count, group) => count + group.count, 0);

  const toggleYear = (archiveYear: number) => {
    const key = String(archiveYear);
    setOpenYears((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Archive</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            {isMonthArchive ? `${year} 年 ${String(month).padStart(2, "0")} 月` : "归档"}
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>
            {isMonthArchive ? `${loading ? "正在整理" : "共"} ${total} 篇文章` : `共 ${archiveArticleCount} 篇文章，跨越 ${archiveYears.length} 年`}
          </p>
        </div>

        <div className="max-w-3xl">
          {isMonthArchive ? (
            <div className="flex flex-col gap-0">
              {posts.map((post, idx) => (
                <Link
                  key={post.id}
                  to={`/posts/${post.slug}`}
                  className="flex items-center gap-4 px-6 py-4 border-b group hover:bg-section-bg transition-colors"
                  style={{
                    borderColor: "var(--warm-border)",
                    borderTopWidth: idx === 0 ? "1px" : "0",
                  }}
                >
                  <div className="shrink-0 text-center w-12">
                    <div
                      className="text-xs"
                      style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)", lineHeight: 1.2 }}
                    >
                      {toDateText(post.publishedAt).slice(5).replace("-", "/")}
                    </div>
                  </div>

                  <div className="w-px h-6 shrink-0" style={{ background: "var(--warm-border)" }} />

                  <div className="flex-1 min-w-0">
                    <h3
                      className="leading-snug group-hover:opacity-70 transition-opacity"
                      style={{ fontFamily: "var(--fontDisplay)", fontSize: "15px", color: "var(--ink)" }}
                    >
                      {post.title}
                    </h3>
                  </div>

                  <div className="hidden md:flex items-center gap-3 shrink-0">
                    {post.category && (
                      <span
                        className="text-xs px-2 py-0.5 rounded-sm"
                        style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}
                      >
                        {post.category.name}
                      </span>
                    )}
                    <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                      <ClockIcon size={11} />{post.readTime}min
                    </span>
                  </div>
                </Link>
              ))}

              {posts.length === 0 && (
                <div className="py-20 text-center">
                  <CalendarIcon size={32} className="mx-auto mb-4" style={{ color: "var(--warm-border)" }} />
                  <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅归档..." : "这个月份暂时还没有文章"}</p>
                  <Link to="/archive" className="inline-block mt-4 text-xs underline" style={{ color: "var(--muted-ink)", textUnderlineOffset: "4px" }}>
                    返回总归档
                  </Link>
                </div>
              )}

              <AppPagination page={page} totalPages={postTotalPages} onPageChange={setPage} />
            </div>
          ) : (
            <>
              {displayedArchiveYears.map((group) => {
                const isOpen = openYears[String(group.year)] ?? defaultOpenYears[String(group.year)] ?? false;
                return (
                  <div key={group.year} className="mb-2">
                    {/* Year header / accordion toggle */}
                    <button
                      onClick={() => toggleYear(group.year)}
                      className="w-full flex items-center justify-between py-4 px-6 border hover:opacity-80 transition-opacity"
                      style={{
                        background: isOpen ? "var(--ink)" : "var(--warm-white)",
                        borderColor: "var(--warm-border)",
                        color: isOpen ? "var(--warm-white)" : "var(--ink)",
                      }}
                    >
                      <span style={{ fontFamily: "var(--fontDisplay)", fontSize: "24px", fontWeight: 400 }}>
                        {group.year}
                      </span>
                      <div className="flex items-center gap-3">
                        <span
                          className="text-xs"
                          style={{ opacity: 0.7 }}
                        >
                          {group.count} 篇
                        </span>
                        {isOpen ? (
                          <ChevronUpIcon size={16} />
                        ) : (
                          <ChevronDownIcon size={16} />
                        )}
                      </div>
                    </button>

                    {/* Month list */}
                    <div className={isOpen ? "block" : "hidden"}>
                      <div
                        className="border border-t-0"
                        style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)" }}
                      >
                        {group.months.map((item, idx) => (
                          <Link
                            key={item.slug}
                            to={`/archive/${group.year}/${String(item.month).padStart(2, "0")}`}
                            className="flex items-center gap-4 px-6 py-4 border-b group hover:bg-section-bg transition-colors"
                            style={{
                              borderColor: "var(--warm-border)",
                              borderBottomWidth: idx === group.months.length - 1 ? "0" : "1px",
                            }}
                          >
                            <div className="shrink-0 text-center w-10">
                              <div
                                className="text-xs"
                                style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)", lineHeight: 1.2 }}
                              >
                                {String(item.month).padStart(2, "0")} 月
                              </div>
                            </div>

                            <div className="w-px h-6 shrink-0" style={{ background: "var(--warm-border)" }} />

                            <div className="flex-1 min-w-0">
                              <h3
                                className="leading-snug group-hover:opacity-70 transition-opacity"
                                style={{ fontFamily: "var(--fontDisplay)", fontSize: "15px", color: "var(--ink)" }}
                              >
                                {group.year} 年 {item.month} 月
                              </h3>
                            </div>

                            <span className="text-xs shrink-0" style={{ color: "var(--muted-ink)" }}>
                              {item.count} 篇
                            </span>
                          </Link>
                        ))}
                      </div>
                    </div>
                  </div>
                );
              })}

              {displayedArchiveYears.length === 0 && (
                <div className="py-20 text-center">
                  <CalendarIcon size={32} className="mx-auto mb-4" style={{ color: "var(--warm-border)" }} />
                  <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅归档..." : "归档暂时还是空白"}</p>
                </div>
              )}

              <AppPagination page={page} totalPages={archiveTotalPages} onPageChange={setPage} />
            </>
          )}
        </div>
      </div>
    </PageLayout>
  );
}
