import { Link } from "react-router-dom";
import PageLayout from "../components/layout/PageLayout";
import { useTags } from "../features/taxonomy/useTaxonomy";

export default function Tags() {
  const { items: tags, loading } = useTags();
  const maxCount = Math.max(...tags.map((t) => t.count), 1);
  const minSize = 13;
  const maxSize = 30;

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        {/* Header */}
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Tags</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            标签
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>
            {tags.length} 个标签{loading ? " · 正在更新" : ""}
          </p>
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
              const tagSlug = t.slug || t.name;
              return (
                <Link
                  key={t.id || tagSlug}
                  to={`/tag/${encodeURIComponent(tagSlug)}`}
                  className="transition-all duration-200 hover:opacity-60"
                  style={{
                    fontSize: `${fontSize}px`,
                    fontFamily: "var(--fontDisplay)",
                    fontWeight: fontSize > 22 ? 700 : 400,
                    color: "var(--ink)",
                    cursor: "pointer",
                    paddingBottom: "2px",
                    borderBottom: "2px solid transparent",
                  }}
                >
                  {t.name}
                  <sup className="text-xs ml-0.5" style={{ fontSize: "10px" }}>{t.count}</sup>
                </Link>
              );
            })}
          </div>
        </div>

        {/* Default: list all tags with posts count */}
        <div className="block">
          <div className="text-xs tracking-widest uppercase mb-6" style={{ color: "var(--muted-ink)" }}>所有标签</div>
          <div className="flex flex-wrap gap-2">
            {tags.map((t) => {
              const tagSlug = t.slug || t.name;
              return (
                <Link
                  key={t.id || tagSlug}
                  to={`/tag/${encodeURIComponent(tagSlug)}`}
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
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </PageLayout>
  );
}
