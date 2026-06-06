import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CalendarIcon, ClockIcon, TagIcon, ArrowLeftIcon, Share2Icon, BookmarkIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import { posts } from "../data/mockData";
import { toast } from "sonner";

// Show the first post as default (static route, no dynamic params)
const DEMO_POST = posts[0];

export default function BlogDetail() {
  const navigate = useNavigate();
  const [bookmarked, setBookmarked] = useState(false);

  const post = DEMO_POST;
  const related = posts.filter((p) => p.id !== post.id && p.category === post.category).slice(0, 3);

  const handleShare = () => {
    toast.success("链接已复制到剪贴板");
  };

  const handleBookmark = () => {
    setBookmarked((v) => !v);
    toast.success(bookmarked ? "已取消收藏" : "已收藏文章");
  };

  return (
    <PageLayout>
      <article className="max-w-7xl mx-auto px-8 pt-12 pb-24">
        {/* Back */}
        <button
          onClick={() => navigate("/blog")}
          className="inline-flex items-center gap-1.5 text-sm mb-10 hover:opacity-60 transition-opacity"
          style={{ color: "var(--muted-ink)" }}
        >
          <ArrowLeftIcon size={14} /> 返回文章列表
        </button>

        {/* Layout: left (article) + right (sidebar) */}
        <div className="flex gap-16 flex-wrap md:flex-nowrap">
          {/* Article */}
          <div className="flex-1 min-w-0">
            {/* Cover */}
            <div className="overflow-hidden mb-10" style={{ maxHeight: "460px" }}>
              <img
                src={post.coverImage}
                alt={post.title}
                className="w-full h-full object-cover"
                style={{ maxHeight: "460px" }}
              />
            </div>

            {/* Meta */}
            <div className="flex items-center gap-3 flex-wrap mb-5">
              <span
                className="text-xs px-2.5 py-1 rounded-sm"
                style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}
              >
                {post.category}
              </span>
              <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                <CalendarIcon size={11} />{post.date}
              </span>
              <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                <ClockIcon size={11} />{post.readTime} 分钟阅读
              </span>
            </div>

            {/* Title */}
            <h1
              className="mb-8 leading-tight"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(28px,3.5vw,44px)", fontWeight: 400, color: "var(--ink)", lineHeight: 1.25 }}
            >
              {post.title}
            </h1>

            {/* Content */}
            <div className="prose-blog" dangerouslySetInnerHTML={{ __html: post.content }} />

            {/* Tags */}
            <div className="flex items-center gap-2 flex-wrap mt-12 pt-8 border-t" style={{ borderColor: "var(--warm-border)" }}>
              <TagIcon size={14} style={{ color: "var(--muted-ink)" }} />
              {post.tags.map((tag) => (
                <Link
                  key={tag}
                  to="/tags"
                  className="text-xs px-3 py-1 rounded-full border hover:border-primary transition-colors"
                  style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)" }}
                >
                  #{tag}
                </Link>
              ))}
            </div>

            {/* Related */}
            {related.length > 0 && (
              <div className="mt-14">
                <h3 className="text-xs tracking-widest uppercase mb-6" style={{ color: "var(--muted-ink)" }}>相关文章</h3>
                <div className="flex flex-col gap-0">
                  {related.map((rp) => (
                    <Link
                      key={rp.id}
                      to="/blog"
                      className="flex gap-4 py-4 border-b hover:opacity-70 transition-opacity group flex-wrap"
                      style={{ borderColor: "var(--warm-border)" }}
                    >
                      <div className="overflow-hidden shrink-0 w-20 h-14">
                        <img src={rp.coverImage.replace("w=1200", "w=200")} alt="" className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-xs mb-1" style={{ color: "var(--muted-ink)" }}>{rp.date}</div>
                        <div
                          className="leading-snug"
                          style={{ fontFamily: "var(--fontDisplay)", fontSize: "14px", color: "var(--ink)" }}
                        >
                          {rp.title}
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Sidebar */}
          <aside className="w-full md:w-52 shrink-0">
            {/* Actions */}
            <div className="flex gap-2 mb-8">
              <button
                onClick={handleShare}
                className="flex items-center gap-1.5 text-xs px-3 py-2 border rounded-sm transition-colors hover:border-primary"
                style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
              >
                <Share2Icon size={13} /> 分享
              </button>
              <button
                onClick={handleBookmark}
                className="flex items-center gap-1.5 text-xs px-3 py-2 border rounded-sm transition-colors"
                style={{
                  borderColor: bookmarked ? "var(--olive)" : "var(--warm-border)",
                  color: bookmarked ? "var(--olive)" : "var(--muted-ink)",
                  fontFamily: "var(--fontSans)",
                }}
              >
                <BookmarkIcon size={13} /> {bookmarked ? "已收藏" : "收藏"}
              </button>
            </div>

            {/* TOC (static) */}
            <div
              className="p-5"
              style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)" }}
            >
              <div className="text-xs tracking-widest uppercase mb-4" style={{ color: "var(--muted-ink)" }}>目录</div>
              <div className="flex flex-col gap-3">
                <div className="text-xs" style={{ color: "var(--ink)" }}>一、序言</div>
                <div className="text-xs pl-3" style={{ color: "var(--muted-ink)" }}>· 引子</div>
                <div className="text-xs" style={{ color: "var(--ink)" }}>二、正文展开</div>
                <div className="text-xs pl-3" style={{ color: "var(--muted-ink)" }}>· 细节描述</div>
                <div className="text-xs pl-3" style={{ color: "var(--muted-ink)" }}>· 深入探讨</div>
                <div className="text-xs" style={{ color: "var(--ink)" }}>三、尾声</div>
              </div>
            </div>

            {/* All posts link */}
            <Link
              to="/blog"
              className="mt-6 block text-xs text-center py-3 border hover:border-primary transition-colors rounded-sm"
              style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)" }}
            >
              查看所有文章
            </Link>
          </aside>
        </div>
      </article>
    </PageLayout>
  );
}
