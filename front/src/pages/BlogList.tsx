import { Link, useLocation, useParams, useSearchParams } from "react-router-dom";
import { CalendarIcon, ClockIcon, SearchIcon } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import PageLayout from "../components/layout/PageLayout";
import { AppPagination } from "../components/ui/app-pagination";
import { blogApi } from "../features/blog/blogApi";
import { usePosts } from "../features/blog/usePosts";
import { useCategories, useTags } from "../features/taxonomy/useTaxonomy";
import { findTaxonomyItemByRouteParam } from "../features/taxonomy/taxonomyMapper";
import { useNormalizePage, usePage } from "../hooks/usePage";
import { toDateText } from "../lib/text";

const PAGE_SIZE = 10;
type SortMode = "default" | "hot" | "total";

export default function BlogList() {
  const { slug } = useParams();
  const location = useLocation();
  const decodedSlug = slug ? decodeURIComponent(slug) : undefined;
  const isTagRoute = location.pathname.startsWith("/tag/");
  const selectedCategory = !isTagRoute ? decodedSlug : undefined;
  const selectedTag = isTagRoute ? decodedSlug : undefined;
  const { page, setPage } = usePage();
  const [searchParams, setSearchParams] = useSearchParams();
  const sortMode: SortMode = (searchParams.get("sort") as SortMode) || "default";

  const setSortMode = (mode: SortMode) => {
    const next = new URLSearchParams(searchParams);
    if (mode === "default") next.delete("sort");
    else next.set("sort", mode);
    setSearchParams(next, { replace: true });
  };

  /* Always call all three hooks (React rules) */
  const defaultResult = usePosts({
    category: sortMode === "default" ? selectedCategory : undefined,
    tag: sortMode === "default" ? selectedTag : undefined,
    page,
    pageSize: PAGE_SIZE,
  });
  const hotResult = useQuery({
    queryKey: ["hotPosts", "recent"],
    queryFn: () => blogApi.listHotPosts("recent", 20),
    enabled: sortMode === "hot",
  });
  const totalResult = useQuery({
    queryKey: ["hotPosts", "total"],
    queryFn: () => blogApi.listHotPosts("total", 20),
    enabled: sortMode === "total",
  });

  const filtered =
    sortMode === "hot" ? (hotResult.data?.posts ?? [])
    : sortMode === "total" ? (totalResult.data?.posts ?? [])
    : defaultResult.posts;
  const total =
    sortMode === "hot" ? 20
    : sortMode === "total" ? 20
    : defaultResult.total;
  const loading =
    sortMode === "hot" ? hotResult.isLoading
    : sortMode === "total" ? totalResult.isLoading
    : defaultResult.loading;
  const totalPages = sortMode === "default" ? Math.ceil(total / PAGE_SIZE) : 1;

  useNormalizePage(page, setPage, totalPages, loading);

  const { items: categories } = useCategories();
  const { items: tags } = useTags();
  const matchedTag = isTagRoute ? findTaxonomyItemByRouteParam(tags, decodedSlug) : undefined;
  const tagDisplayName = matchedTag?.name || selectedTag;
  const pageTitle = tagDisplayName ? `标签：${tagDisplayName}` : selectedCategory ? `分类：${selectedCategory}` : sortMode === "hot" ? "热榜" : sortMode === "total" ? "总榜" : "文章";

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Page header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Writing</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            {pageTitle}
          </h1>
        </div>

        {/* Filter bar */}
        <div className="flex items-center gap-3 flex-wrap mb-14 pb-6 border-b" style={{ borderColor: "var(--warm-border)" }}>
          <Link
            to="/blog"
            className="px-4 py-1.5 text-xs rounded-full transition-all duration-200"
            style={{
              fontFamily: "var(--fontSans)",
              letterSpacing: "0.04em",
              background: !selectedCategory && !selectedTag ? "var(--ink)" : "transparent",
              color: !selectedCategory && !selectedTag ? "var(--warm-white)" : "var(--muted-ink)",
              border: `1px solid ${!selectedCategory && !selectedTag ? "var(--ink)" : "var(--warm-border)"}`,
            }}
          >
            全部
          </Link>

          {/* Ranking tags */}
          <button
            type="button"
            onClick={() => setSortMode("hot")}
            className="px-4 py-1.5 text-xs rounded-full transition-all duration-200"
            style={{
              fontFamily: "var(--fontSans)",
              letterSpacing: "0.04em",
              background: sortMode === "hot" ? "var(--ink)" : "transparent",
              color: sortMode === "hot" ? "var(--warm-white)" : "var(--muted-ink)",
              border: `1px solid ${sortMode === "hot" ? "var(--ink)" : "var(--warm-border)"}`,
            }}
          >
            热榜
          </button>
          <button
            type="button"
            onClick={() => setSortMode("total")}
            className="px-4 py-1.5 text-xs rounded-full transition-all duration-200"
            style={{
              fontFamily: "var(--fontSans)",
              letterSpacing: "0.04em",
              background: sortMode === "total" ? "var(--ink)" : "transparent",
              color: sortMode === "total" ? "var(--warm-white)" : "var(--muted-ink)",
              border: `1px solid ${sortMode === "total" ? "var(--ink)" : "var(--warm-border)"}`,
            }}
          >
            总榜
          </button>

          <span className="w-px h-4" style={{ background: "var(--warm-border)" }} />

          {sortMode === "default" && categories.map((cat) => {
            const categorySlug = cat.slug || cat.name;
            const isActive = selectedCategory === categorySlug || selectedCategory === cat.name;
            return (
              <Link
                key={cat.id || categorySlug}
                to={`/category/${encodeURIComponent(categorySlug)}`}
                className="px-4 py-1.5 text-xs rounded-full transition-all duration-200"
                style={{
                  fontFamily: "var(--fontSans)",
                  letterSpacing: "0.04em",
                  background: isActive ? "var(--ink)" : "transparent",
                  color: isActive ? "var(--warm-white)" : "var(--muted-ink)",
                  border: `1px solid ${isActive ? "var(--ink)" : "var(--warm-border)"}`,
                }}
              >
                {cat.name}
              </Link>
            );
          })}
          <span className="ml-auto text-xs" style={{ color: "var(--muted-ink)" }}>{loading ? "..." : total} 篇</span>
        </div>

        {/* Articles grid */}
        <div className="flex flex-col gap-0">
          {filtered.map((post, idx) => (
            <Link
              key={post.id}
              to={`/posts/${post.slug}`}
              className="group flex gap-8 py-8 border-b hover:bg-card transition-colors duration-200 -mx-6 px-6 rounded-sm flex-wrap md:flex-nowrap"
              style={{ borderColor: "var(--warm-border)" }}
            >
              {/* Number */}
              <div
                className="shrink-0 pt-1 text-xs"
                style={{ color: "var(--warm-border)", fontFamily: "var(--fontDisplay)", fontSize: "13px", minWidth: "32px" }}
              >
                {String(idx + 1).padStart(2, "0")}
              </div>

              {/* Cover */}
              {post.coverUrl && (
                <div className="shrink-0 overflow-hidden w-full md:w-40 h-28 md:h-24">
                  <img
                    src={post.coverUrl.replace("w=1200", "w=400")}
                    alt={post.title}
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
              )}

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-3 mb-2 flex-wrap">
                  {post.category && (
                    <span
                      className="text-xs px-2 py-0.5 rounded-sm"
                      style={{ background: "var(--section-bg)", color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                    >
                      {post.category.name}
                    </span>
                  )}
                  <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                    <CalendarIcon size={11} />{toDateText(post.publishedAt)}
                  </span>
                  <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                    <ClockIcon size={11} />{post.readTime} 分钟
                  </span>
                </div>
                <h2
                  className="leading-snug mb-2 group-hover:opacity-70 transition-opacity"
                  style={{ fontFamily: "var(--fontDisplay)", fontSize: "19px", fontWeight: 500, color: "var(--ink)" }}
                >
                  {post.title}
                </h2>
                <p className="text-sm leading-relaxed line-clamp-2" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                  {post.summary}
                </p>
              </div>

              {/* Arrow */}
              <div className="hidden md:flex items-center shrink-0" style={{ color: "var(--warm-border)" }}>
                →
              </div>
            </Link>
          ))}
        </div>

        {filtered.length === 0 && (
          <div className="text-center py-20">
            <SearchIcon size={32} className="mx-auto mb-4" style={{ color: "var(--warm-border)" }} />
            <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅文章..." : "这里暂时还没有文章"}</p>
          </div>
        )}

        <AppPagination page={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
    </PageLayout>
  );
}
