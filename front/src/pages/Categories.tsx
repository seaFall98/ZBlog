import { Link } from "react-router-dom";
import { ArrowRightIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { useCategories } from "../features/taxonomy/useTaxonomy";

export default function Categories() {
  const { items: categories, loading } = useCategories();
  const totalPosts = categories.reduce((sum, category) => sum + category.count, 0);

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Categories</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            分类
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>
            {categories.length} 个分类 · {totalPosts} 篇文章{loading ? " · 正在更新" : ""}
          </p>
        </div>

        {/* Grid */}
        <div className="flex flex-wrap gap-6">
          {categories.map((cat) => {
            const categorySlug = cat.slug || cat.name;
            return (
              <Link
                key={cat.id || categorySlug}
                to={`/category/${encodeURIComponent(categorySlug)}`}
                className="group flex-1 min-w-72 block overflow-hidden hover:-translate-y-1 transition-transform duration-300"
                style={{ border: "1px solid var(--warm-border)" }}
              >
                {/* Cover */}
                <div className="overflow-hidden h-44" style={{ background: "var(--section-bg)" }}>
                  {cat.coverUrl ? (
                    <img
                      src={cat.coverUrl}
                      alt={cat.name}
                      loading="lazy"
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center px-6 text-center text-xs tracking-widest uppercase" style={{ color: "var(--muted-ink)" }}>
                      {cat.name}
                    </div>
                  )}
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
                </div>
              </Link>
            );
          })}
        </div>
      </div>
    </PageLayout>
  );
}
