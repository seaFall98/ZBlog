import { useState } from "react";
import { ChevronDownIcon, ExternalLinkIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { useFriendLinks } from "../features/links/useFriendLinks";
import { apiClient } from "../lib/apiClient";
import { toast } from "sonner";

export default function Links() {
  const { links, types, loading } = useFriendLinks();
  const categories = Array.from(new Set(links.map((l) => l.category)));
  const [activeCategory, setActiveCategory] = useState("全部");
  const [formOpen, setFormOpen] = useState(false);
  const [formSubmitting, setFormSubmitting] = useState(false);
  const [form, setForm] = useState({ name: "", url: "", description: "", type_id: "", avatar: "" });

  const filtered = activeCategory === "全部"
    ? links
    : links.filter((l) => l.category === activeCategory);

  const handleApply = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim() || !form.url.trim() || !form.description.trim() || !form.type_id) {
      toast.error("请填写网站名称、地址、简介和分类");
      return;
    }
    setFormSubmitting(true);
    try {
      await apiClient.post("/friends/apply", {
        name: form.name.trim(),
        url: form.url.trim(),
        description: form.description.trim(),
        type_id: Number(form.type_id),
        avatar: form.avatar.trim() || undefined,
      });
      toast.success("申请已提交，审核中");
      setForm({ name: "", url: "", description: "", type_id: "", avatar: "" });
      setFormOpen(false);
    } catch {
      toast.error("申请提交失败，请稍后再试");
    } finally {
      setFormSubmitting(false);
    }
  };

  return (
    <PageLayout>
      <div className="max-w-7xl mx-auto px-8 pt-16 pb-24">
        <div className="mb-14">
          <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Friends</p>
          <h1 style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(36px,4vw,56px)", fontWeight: 400, color: "var(--ink)" }}>
            友情链接
          </h1>
          <p className="mt-4 text-sm" style={{ color: "var(--muted-ink)" }}>{loading ? "正在加载友链..." : "认识有趣的人，看见不一样的风景"}</p>
        </div>

        <div className="flex gap-2 flex-wrap mb-12 pb-6 border-b" style={{ borderColor: "var(--warm-border)" }}>
          {["全部", ...categories].map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className="px-4 py-1.5 text-xs rounded-full transition-all"
              style={{
                background: activeCategory === cat ? "var(--ink)" : "transparent",
                color: activeCategory === cat ? "var(--warm-white)" : "var(--muted-ink)",
                border: `1px solid ${activeCategory === cat ? "var(--ink)" : "var(--warm-border)"}`,
                fontFamily: "var(--fontSans)",
              }}
            >
              {cat}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap gap-4">
          {filtered.map((link) => (
            <a
              key={link.id}
              href={link.url}
              target="_blank"
              rel="noopener noreferrer"
              className="group flex items-center gap-4 p-5 hover:-translate-y-1 transition-transform duration-300"
              style={{ background: "var(--warm-white)", border: "1px solid var(--warm-border)", flexBasis: "calc(33% - 12px)", minWidth: "260px" }}
            >
              <div className="w-12 h-12 shrink-0 overflow-hidden flex items-center justify-center rounded-full" style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}>
                {link.logo ? (
                  <img
                    src={link.logo}
                    alt={link.name}
                    loading="lazy"
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      const target = e.currentTarget;
                      target.style.display = "none";
                      const parent = target.parentElement;
                      if (parent) parent.innerHTML = link.name.slice(0, 2);
                    }}
                  />
                ) : (
                  link.name.slice(0, 2)
                )}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-medium text-sm" style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}>{link.name}</span>
                  <ExternalLinkIcon size={12} className="opacity-0 group-hover:opacity-100 transition-opacity" style={{ color: "var(--muted-ink)" }} />
                </div>
                <p className="text-xs line-clamp-2 leading-relaxed" style={{ color: "var(--muted-ink)" }}>{link.description || "一位认真记录生活的朋友。"}</p>
                <span className="inline-block mt-2 text-xs px-2 py-0.5 rounded-sm" style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}>{link.category}</span>
              </div>
            </a>
          ))}
        </div>

        {filtered.length === 0 && (
          <div className="py-20 text-center">
            <p style={{ color: "var(--muted-ink)" }}>{loading ? "正在翻阅友链..." : "友情链接暂时还是空白"}</p>
          </div>
        )}

        <div className="mt-16 p-10 text-center" style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)" }}>
          <h2 className="mb-3" style={{ fontFamily: "var(--fontDisplay)", fontSize: "22px", fontWeight: 400, color: "var(--ink)" }}>申请友链</h2>
          <p className="text-sm mb-6" style={{ color: "var(--muted-ink)" }}>
            欢迎志同道合的朋友申请友链，请在留言墙留下你的博客信息
          </p>
          <a href="/guestbook" className="inline-flex items-center gap-2 text-sm px-6 py-3 transition-opacity hover:opacity-80" style={{ background: "var(--ink)", color: "var(--warm-white)", fontFamily: "var(--fontSans)" }}>
            前往留言墙 →
          </a>
        </div>

        {/* Collapsible application form */}
        <div className="mt-6 p-8" style={{ border: "1px solid var(--warm-border)", background: "var(--warm-white)" }}>
          <button
            type="button"
            onClick={() => setFormOpen(!formOpen)}
            className="flex items-center gap-2 text-sm font-medium w-full text-left"
            style={{ color: "var(--ink)", fontFamily: "var(--fontSans)" }}
          >
            <ChevronDownIcon
              size={14}
              className="transition-transform"
              style={{ transform: formOpen ? "rotate(0deg)" : "rotate(-90deg)" }}
            />
            直接申请
          </button>

          {formOpen && (
            <form onSubmit={handleApply} className="mt-6 grid gap-5 max-w-lg">
              <label className="grid gap-1.5">
                <span className="text-xs" style={{ color: "var(--muted-ink)" }}>网站名称 *</span>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="px-3 py-2 text-sm border"
                  style={{ borderColor: "var(--warm-border)", background: "var(--ivory)", color: "var(--ink)", outline: "none" }}
                  required
                />
              </label>
              <label className="grid gap-1.5">
                <span className="text-xs" style={{ color: "var(--muted-ink)" }}>网站地址 *</span>
                <input
                  type="url"
                  value={form.url}
                  onChange={(e) => setForm({ ...form, url: e.target.value })}
                  className="px-3 py-2 text-sm border"
                  style={{ borderColor: "var(--warm-border)", background: "var(--ivory)", color: "var(--ink)", outline: "none" }}
                  placeholder="https://"
                  required
                />
              </label>
              <label className="grid gap-1.5">
                <span className="text-xs" style={{ color: "var(--muted-ink)" }}>简介 *</span>
                <textarea
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  rows={3}
                  className="px-3 py-2 text-sm border resize-y"
                  style={{ borderColor: "var(--warm-border)", background: "var(--ivory)", color: "var(--ink)", outline: "none" }}
                  required
                />
              </label>
              <label className="grid gap-1.5">
                <span className="text-xs" style={{ color: "var(--muted-ink)" }}>友链分类 *</span>
                <select
                  value={form.type_id}
                  onChange={(e) => setForm({ ...form, type_id: e.target.value })}
                  className="px-3 py-2 text-sm border"
                  style={{ borderColor: "var(--warm-border)", background: "var(--ivory)", color: "var(--ink)", outline: "none" }}
                  required
                >
                  <option value="">请选择</option>
                  {types.map((t) => (
                    <option key={t.id} value={t.id}>{t.name}</option>
                  ))}
                </select>
              </label>
              <label className="grid gap-1.5">
                <span className="text-xs" style={{ color: "var(--muted-ink)" }}>Logo 地址（选填）</span>
                <input
                  type="url"
                  value={form.avatar}
                  onChange={(e) => setForm({ ...form, avatar: e.target.value })}
                  className="px-3 py-2 text-sm border"
                  style={{ borderColor: "var(--warm-border)", background: "var(--ivory)", color: "var(--ink)", outline: "none" }}
                  placeholder="https://"
                />
              </label>
              <button
                type="submit"
                disabled={formSubmitting}
                className="w-fit px-5 py-2.5 text-sm transition-opacity hover:opacity-80 disabled:opacity-50"
                style={{ background: "var(--ink)", color: "var(--warm-white)", fontFamily: "var(--fontSans)" }}
              >
                {formSubmitting ? "提交中..." : "提交申请"}
              </button>
            </form>
          )}
        </div>
      </div>
    </PageLayout>
  );
}
