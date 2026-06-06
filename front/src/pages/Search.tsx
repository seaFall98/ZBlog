import { useState } from "react";
import { Link } from "react-router-dom";
import { SearchIcon, CalendarIcon, ClockIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { posts } from "../data/mockData";

export default function Search() {
  const [query, setQuery] = useState("");

  const results = query.trim().length > 0
    ? posts.filter(
        (p) =>
          p.title.toLowerCase().includes(query.toLowerCase()) ||
          p.excerpt.toLowerCase().includes(query.toLowerCase()) ||
          p.tags.some((t) => t.toLowerCase().includes(query.toLowerCase())) ||
          p.category.toLowerCase().includes(query.toLowerCase())
      )
    : [];

  const highlight = (text: string) => {
    if (!query.trim()) return text;
    const idx = text.toLowerCase().indexOf(query.toLowerCase());
    if (idx === -1) return text;
    return (
      text.slice(0, idx) +
      `<mark style="background:var(--section-bg);color:var(--olive);padding:0 2px;">${text.slice(idx, idx + query.length)}</mark>` +
      text.slice(idx + query.length)
    );
  };

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Search</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            搜索
          </h1>
        </div>

        {/* Search box */}
        <div
          className="flex items-center gap-4 px-6 py-4 mb-12 max-w-2xl"
          style={{ border: "1px solid var(--warm-border)", background: "var(--warm-white)" }}
        >
          <SearchIcon size={20} style={{ color: "var(--muted-ink)" }} />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="输入关键词搜索文章..."
            className="flex-1 bg-transparent outline-none text-base"
            style={{ fontFamily: "var(--fontSans)", color: "var(--ink)" }}
            autoFocus
          />
          {query && (
            <button
              onClick={() => setQuery("")}
              className="text-xs px-2 py-1 hover:opacity-60 transition-opacity"
              style={{ color: "var(--muted-ink)" }}
            >
              清除
            </button>
          )}
        </div>

        {/* Results */}
        {query.trim().length > 0 && (
          <div>
            <p className="text-xs mb-6" style={{ color: "var(--muted-ink)" }}>
              「{query}」的搜索结果：{results.length} 篇
            </p>

            {results.length === 0 && (
              <div className="py-20 text-center">
                <div className="text-5xl mb-6" style={{ opacity: 0.15 }}>∅</div>
                <p style={{ color: "var(--muted-ink)" }}>未找到相关文章</p>
                <p className="text-sm mt-2" style={{ color: "var(--muted-ink)" }}>请尝试其他关键词</p>
              </div>
            )}

            <div className="flex flex-col gap-0">
              {results.map((post) => (
                <Link
                  key={post.id}
                  to={`/blog/${post.id}`}
                  className="flex gap-6 py-6 border-b group hover:opacity-80 transition-opacity"
                  style={{ borderColor: "var(--warm-border)" }}
                >
                  {post.coverImage && (
                    <div className="shrink-0 overflow-hidden w-24 h-16">
                      <img
                        src={post.coverImage.replace("w=1200", "w=200")}
                        alt=""
                        loading="lazy"
                        className="w-full h-full object-cover"
                      />
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-2">
                      <span
                        className="text-xs px-2 py-0.5 rounded-sm"
                        style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}
                      >
                        {post.category}
                      </span>
                      <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                        <CalendarIcon size={11} />{post.date}
                      </span>
                    </div>
                    <h2
                      className="leading-snug mb-1"
                      style={{ fontFamily: "var(--fontDisplay)", fontSize: "17px", color: "var(--ink)" }}
                      dangerouslySetInnerHTML={{ __html: highlight(post.title) }}
                    />
                    <p
                      className="text-sm line-clamp-2"
                      style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
                      dangerouslySetInnerHTML={{ __html: highlight(post.excerpt) }}
                    />
                  </div>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Suggestions when empty */}
        {query.trim().length === 0 && (
          <div>
            <div className="text-xs tracking-widest uppercase mb-5" style={{ color: "var(--muted-ink)" }}>热门搜索</div>
            <div className="flex flex-wrap gap-2">
              {["旅行", "摄影", "阅读", "生活", "日本", "音乐", "咖啡"].map((kw) => (
                <button
                  key={kw}
                  onClick={() => setQuery(kw)}
                  className="px-4 py-2 text-sm border rounded-full hover:border-primary transition-colors"
                  style={{
                    borderColor: "var(--warm-border)",
                    color: "var(--muted-ink)",
                    fontFamily: "var(--fontSans)",
                  }}
                >
                  {kw}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </PageLayout>
  );
}
