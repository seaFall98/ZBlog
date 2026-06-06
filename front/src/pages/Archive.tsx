import { useState } from "react";
import { Link } from "react-router-dom";
import { ChevronDownIcon, ChevronUpIcon, CalendarIcon, ClockIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { posts } from "../data/mockData";

type YearGroup = {
  year: string;
  posts: typeof posts;
};

function groupByYear(allPosts: typeof posts): YearGroup[] {
  const map: Record<string, typeof posts> = {};
  allPosts.forEach((p) => {
    const year = p.date.split("-")[0] ?? "未知";
    if (!map[year]) map[year] = [];
    map[year].push(p);
  });
  return Object.keys(map)
    .sort((a, b) => parseInt(b) - parseInt(a))
    .map((year) => ({ year, posts: map[year] ?? [] }));
}

export default function Archive() {
  const yearGroups = groupByYear(posts);
  const [openYears, setOpenYears] = useState<Record<string, boolean>>(() => {
    const init: Record<string, boolean> = {};
    yearGroups.forEach((g, i) => { init[g.year] = i === 0; });
    return init;
  });

  const toggleYear = (year: string) => {
    setOpenYears((prev) => ({ ...prev, [year]: !prev[year] }));
  };

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Archive</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            归档
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>共 {posts.length} 篇文章，跨越 {yearGroups.length} 年</p>
        </div>

        {/* Timeline */}
        <div className="max-w-3xl">
          {yearGroups.map((group) => (
            <div key={group.year} className="mb-2">
              {/* Year header / accordion toggle */}
              <button
                onClick={() => toggleYear(group.year)}
                className="w-full flex items-center justify-between py-4 px-6 border hover:opacity-80 transition-opacity"
                style={{
                  background: openYears[group.year] ? "var(--ink)" : "var(--warm-white)",
                  borderColor: "var(--warm-border)",
                  color: openYears[group.year] ? "var(--warm-white)" : "var(--ink)",
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
                    {group.posts.length} 篇
                  </span>
                  {openYears[group.year] ? (
                    <ChevronUpIcon size={16} />
                  ) : (
                    <ChevronDownIcon size={16} />
                  )}
                </div>
              </button>

              {/* Post list */}
              <div className={openYears[group.year] ? "block" : "hidden"}>
                <div
                  className="border border-t-0"
                  style={{ borderColor: "var(--warm-border)", background: "var(--warm-white)" }}
                >
                  {group.posts.map((post, idx) => (
                    <Link
                      key={post.id}
                      to={`/blog/${post.id}`}
                      className="flex items-center gap-4 px-6 py-4 border-b group hover:bg-section-bg transition-colors"
                      style={{
                        borderColor: "var(--warm-border)",
                        borderBottomWidth: idx === group.posts.length - 1 ? "0" : "1px",
                      }}
                    >
                      {/* Month/day */}
                      <div className="shrink-0 text-center w-10">
                        <div
                          className="text-xs"
                          style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)", lineHeight: 1.2 }}
                        >
                          {post.date.slice(5).replace("-", "/")}
                        </div>
                      </div>

                      <div className="w-px h-6 shrink-0" style={{ background: "var(--warm-border)" }} />

                      {/* Title */}
                      <div className="flex-1 min-w-0">
                        <h3
                          className="leading-snug group-hover:opacity-70 transition-opacity"
                          style={{ fontFamily: "var(--fontDisplay)", fontSize: "15px", color: "var(--ink)" }}
                        >
                          {post.title}
                        </h3>
                      </div>

                      {/* Meta */}
                      <div className="hidden md:flex items-center gap-3 shrink-0">
                        <span
                          className="text-xs px-2 py-0.5 rounded-sm"
                          style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}
                        >
                          {post.category}
                        </span>
                        <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                          <ClockIcon size={11} />{post.readTime}min
                        </span>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </PageLayout>
  );
}
