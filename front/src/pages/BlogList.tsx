import { useState } from "react";
import { Link } from "react-router-dom";
import { CalendarIcon, ClockIcon, SearchIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { posts, categories } from "../data/mockData";

export default function BlogList() {
  const [activeCategory, setActiveCategory] = useState("全部");

  const allCategories = ["全部", ...categories.map((c) => c.name)];

  const filtered = activeCategory === "全部"
    ? posts
    : posts.filter((p) => p.category === activeCategory);

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Page header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Writing</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            文章
          </h1>
        </div>

        {/* Filter bar */}
        <div className="flex items-center gap-3 flex-wrap mb-14 pb-6 border-b" style={{ borderColor: "var(--warm-border)" }}>
          {allCategories.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className="px-4 py-1.5 text-xs rounded-full transition-all duration-200"
              style={{
                fontFamily: "var(--fontSans)",
                letterSpacing: "0.04em",
                background: activeCategory === cat ? "var(--ink)" : "transparent",
                color: activeCategory === cat ? "var(--warm-white)" : "var(--muted-ink)",
                border: `1px solid ${activeCategory === cat ? "var(--ink)" : "var(--warm-border)"}`,
              }}
            >
              {cat}
            </button>
          ))}
          <span className="ml-auto text-xs" style={{ color: "var(--muted-ink)" }}>{filtered.length} 篇</span>
        </div>

        {/* Articles grid */}
        <div className="flex flex-col gap-0">
          {filtered.map((post, idx) => (
            <Link
              key={post.id}
              to={`/blog/${post.id}`}
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
              {post.coverImage && (
                <div className="shrink-0 overflow-hidden w-full md:w-40 h-28 md:h-24">
                  <img
                    src={post.coverImage.replace("w=1200", "w=400")}
                    alt={post.title}
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
              )}

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-3 mb-2 flex-wrap">
                  <span
                    className="text-xs px-2 py-0.5 rounded-sm"
                    style={{ background: "var(--section-bg)", color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                  >
                    {post.category}
                  </span>
                  <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                    <CalendarIcon size={11} />{post.date}
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
                  {post.excerpt}
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
            <p style={{ color: "var(--muted-ink)" }}>该分类暂无文章</p>
          </div>
        )}
      </div>
    </PageLayout>
  );
}
