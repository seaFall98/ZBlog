import { useState } from "react";
import { Link } from "react-router-dom";
import PageLayout from "../components/layout/PageLayout";
import { tags, posts } from "../data/mockData";

export default function Tags() {
  const [activeTag, setActiveTag] = useState<string | null>(null);

  const maxCount = Math.max(...tags.map((t) => t.count));
  const minSize = 13;
  const maxSize = 30;

  const filteredPosts = activeTag ? posts.filter((p) => p.tags.includes(activeTag)) : [];

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Tags</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            标签
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>{tags.length} 个标签</p>
        </div>

        {/* Tag cloud */}
        <div
          className="p-10 mb-14"
          style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)" }}
        >
          <div className="flex flex-wrap gap-4 items-center justify-center">
            {tags.map((t) => {
              const ratio = (t.count - 1) / (maxCount - 1 || 1);
              const fontSize = minSize + ratio * (maxSize - minSize);
              const isActive = activeTag === t.name;
              return (
                <button
                  key={t.name}
                  onClick={() => setActiveTag(isActive ? null : t.name)}
                  className="transition-all duration-200 hover:opacity-60"
                  style={{
                    fontSize: `${fontSize}px`,
                    fontFamily: "var(--fontDisplay)",
                    fontWeight: fontSize > 22 ? 700 : 400,
                    color: isActive ? "var(--olive)" : "var(--ink)",
                    opacity: activeTag && !isActive ? 0.3 : 1,
                    cursor: "pointer",
                    paddingBottom: "2px",
                    borderBottom: isActive ? "2px solid var(--olive)" : "2px solid transparent",
                  }}
                >
                  {t.name}
                  <sup className="text-xs ml-0.5" style={{ fontSize: "10px" }}>{t.count}</sup>
                </button>
              );
            })}
          </div>
        </div>

        {/* Selected tag results */}
        <div className={activeTag ? "block" : "hidden"}>
          <h2 className="text-sm mb-6" style={{ color: "var(--muted-ink)" }}>
            # {activeTag} · {filteredPosts.length} 篇
          </h2>
          <div className="flex flex-col gap-0">
            {filteredPosts.map((post) => (
              <Link
                key={post.id}
                to={`/blog/${post.id}`}
                className="flex items-center gap-4 py-4 border-b group hover:opacity-70 transition-opacity"
                style={{ borderColor: "var(--warm-border)" }}
              >
                <span className="text-xs shrink-0 w-20" style={{ color: "var(--muted-ink)" }}>{post.date}</span>
                <span
                  className="flex-1 min-w-0 group-hover:opacity-80 transition-opacity"
                  style={{ fontFamily: "var(--fontDisplay)", fontSize: "15px", color: "var(--ink)" }}
                >
                  {post.title}
                </span>
                <span
                  className="shrink-0 text-xs px-2 py-0.5 rounded-sm"
                  style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}
                >
                  {post.category}
                </span>
              </Link>
            ))}
          </div>
        </div>

        {/* Default: list all tags with posts count */}
        <div className={activeTag ? "hidden" : "block"}>
          <div className="text-xs tracking-widest uppercase mb-6" style={{ color: "var(--muted-ink)" }}>所有标签</div>
          <div className="flex flex-wrap gap-2">
            {tags.map((t) => (
              <button
                key={t.name}
                onClick={() => setActiveTag(t.name)}
                className="px-4 py-2 text-sm border rounded-full hover:border-primary transition-colors"
                style={{
                  borderColor: "var(--warm-border)",
                  color: "var(--muted-ink)",
                  fontFamily: "var(--fontSans)",
                  background: "transparent",
                }}
              >
                #{t.name}
                <span className="ml-1.5 text-xs">{t.count}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
