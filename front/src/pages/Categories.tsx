import { Link } from "react-router-dom";
import { FolderIcon, ArrowRightIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { categories, posts } from "../data/mockData";

export default function Categories() {
  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Categories</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            分类
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>{categories.length} 个分类 · {posts.length} 篇文章</p>
        </div>

        {/* Grid */}
        <div className="flex flex-wrap gap-6">
          {categories.map((cat) => {
            const catPosts = posts.filter((p) => p.category === cat.name).slice(0, 3);
            return (
              <Link
                key={cat.id}
                to="/blog"
                className="group flex-1 min-w-72 block overflow-hidden hover:-translate-y-1 transition-transform duration-300"
                style={{ border: "1px solid var(--warm-border)" }}
              >
                {/* Cover */}
                <div className="overflow-hidden h-44">
                  <img
                    src={cat.coverImage}
                    alt={cat.name}
                    loading="lazy"
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                </div>

                {/* Body */}
                <div className="p-6" style={{ background: "var(--warm-white)" }}>
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <h2
                        style={{ fontFamily: "var(--fontDisplay)", fontSize: "20px", fontWeight: 500, color: "var(--ink)" }}
                      >
                        {cat.name}
                      </h2>
                      <p className="text-xs mt-1" style={{ color: "var(--muted-ink)" }}>{cat.count} 篇</p>
                    </div>
                    <ArrowRightIcon size={16} className="mt-1.5 group-hover:translate-x-1 transition-transform" style={{ color: "var(--muted-ink)" }} />
                  </div>
                  <p className="text-sm leading-relaxed" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
                    {cat.description}
                  </p>

                  {/* Recent posts preview */}
                  {catPosts.length > 0 && (
                    <div className="mt-5 pt-4 border-t flex flex-col gap-1" style={{ borderColor: "var(--warm-border)" }}>
                      {catPosts.map((p) => (
                        <div key={p.id} className="flex items-center gap-2">
                          <span className="shrink-0 w-1 h-1 rounded-full" style={{ background: "var(--muted-ink)" }} />
                          <span className="text-xs truncate" style={{ color: "var(--muted-ink)" }}>{p.title}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </PageLayout>
  );
}
