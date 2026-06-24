import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { CalendarIcon, ClockIcon, EyeIcon, TagIcon, ArrowLeftIcon, Share2Icon, BookmarkIcon } from "lucide-react";
import PageLayout from "../components/layout/PageLayout";
import ArticleContent from "../features/blog/ArticleContent";
import ArticleToc from "../features/blog/ArticleToc";
import type { PostView } from "../features/blog/types";
import { usePost } from "../features/blog/usePost";
import CommentSection from "../features/comments/CommentSection";
import { usePageViewCollector } from "../features/stats/usePageViewCollector";
import { ArticleSeoHead } from "../features/seo/SeoHead";
import { toDateText } from "../lib/text";
import { toast } from "sonner";

function CopyrightNotice({ post }: { post: PostView }) {
  const sourceLabel = post.sourceTitle || post.sourceUrl;

  if (post.copyrightType === "REPOST") {
    return (
      <div className="mt-12 p-5 text-sm leading-relaxed" style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)", color: "var(--muted-ink)" }}>
        本文为转载内容，原文
        {post.sourceUrl ? (
          <a href={post.sourceUrl} target="_blank" rel="noreferrer" className="mx-1 underline underline-offset-4" style={{ color: "var(--ink)" }}>
            {sourceLabel || "链接"}
          </a>
        ) : sourceLabel ? (
          <span className="mx-1" style={{ color: "var(--ink)" }}>{sourceLabel}</span>
        ) : (
          "信息"
        )}
        归原作者所有。{post.copyrightLicense ? `许可协议：${post.copyrightLicense}` : "本站仅作整理与引用。"}
      </div>
    );
  }

  if (post.copyrightType === "TRANSLATION") {
    return (
      <div className="mt-12 p-5 text-sm leading-relaxed" style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)", color: "var(--muted-ink)" }}>
        本文为翻译内容，参考来源
        {post.sourceUrl ? (
          <a href={post.sourceUrl} target="_blank" rel="noreferrer" className="mx-1 underline underline-offset-4" style={{ color: "var(--ink)" }}>
            {sourceLabel || "链接"}
          </a>
        ) : sourceLabel ? (
          <span className="mx-1" style={{ color: "var(--ink)" }}>{sourceLabel}</span>
        ) : (
          "未填写"
        )}
        。{post.copyrightLicense ? `许可协议：${post.copyrightLicense}` : "翻译仅用于学习与交流。"}
      </div>
    );
  }

  return (
    <div className="mt-12 p-5 text-sm leading-relaxed" style={{ background: "var(--section-bg)", border: "1px solid var(--warm-border)", color: "var(--muted-ink)" }}>
      本文为原创文章，转载请注明出处。{post.copyrightLicense ? `许可协议：${post.copyrightLicense}` : ""}
    </div>
  );
}

function ArticleAiSummary({ text }: { text: string }) {
  const [visible, setVisible] = useState("");

  useEffect(() => {
    setVisible("");
    if (!text.trim()) return;
    let index = 0;
    const timer = window.setInterval(() => {
      index += 1;
      setVisible(text.slice(0, index));
      if (index >= text.length) {
        window.clearInterval(timer);
      }
    }, 18);
    return () => window.clearInterval(timer);
  }, [text]);

  if (!text.trim()) return null;

  return (
    <section className="mb-10 border px-5 py-4" style={{ borderColor: "var(--warm-border)", background: "var(--section-bg)" }}>
      <div className="mb-2 text-xs tracking-widest uppercase" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
        AI Summary
      </div>
      <p className="text-sm leading-7" style={{ color: "var(--ink)", fontFamily: "var(--fontBody)" }}>
        {visible}
        {visible.length < text.length && <span aria-hidden="true">|</span>}
      </p>
    </section>
  );
}

export default function BlogDetail() {
  const navigate = useNavigate();
  const { slug } = useParams();
  const { post, related, loading } = usePost(slug ?? "");
  const [bookmarked, setBookmarked] = useState(false);
  const [displayViewCount, setDisplayViewCount] = useState(0);

  useEffect(() => {
    setDisplayViewCount(post?.viewCount ?? 0);
  }, [post?.id, post?.viewCount]);

  usePageViewCollector(post?.id, { onArticleViewCount: setDisplayViewCount });

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

        {loading && !post && (
          <div className="py-24 max-w-2xl">
            <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Loading</p>
            <h1
              className="mb-6 leading-tight"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(28px,3.5vw,44px)", fontWeight: 400, color: "var(--ink)", lineHeight: 1.25 }}
            >
              正在翻开这一页
            </h1>
            <p className="text-sm" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
              正在从 server 读取文章内容与目录。
            </p>
          </div>
        )}

        {!loading && !post && (
          <div className="py-24 max-w-2xl">
            <p className="text-xs tracking-widest uppercase mb-3" style={{ color: "var(--muted-ink)" }}>Missing Page</p>
            <h1
              className="mb-8 leading-tight"
              style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(28px,3.5vw,44px)", fontWeight: 400, color: "var(--ink)", lineHeight: 1.25 }}
            >
              这页纸暂时没有被装订进来
            </h1>
            <p className="text-sm mb-8" style={{ color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}>
              也许它还在整理，先回到文章列表继续阅读。
            </p>
            <Link
              to="/blog"
              className="inline-flex text-xs px-4 py-2 border rounded-sm hover:border-primary transition-colors"
              style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)", fontFamily: "var(--fontSans)" }}
            >
              回到文章列表
            </Link>
          </div>
        )}

        {post && (
          <>
            <ArticleSeoHead post={post} />
            <div className="flex gap-16 flex-wrap md:flex-nowrap">
            {/* Article */}
            <div className="flex-1 min-w-0">
              {/* Cover */}
              {post.coverUrl && (
                <div className="overflow-hidden mb-10" style={{ maxHeight: "460px" }}>
                  <img
                    src={post.coverUrl}
                    alt={post.title}
                    className="w-full h-full object-cover"
                    style={{ maxHeight: "460px" }}
                  />
                </div>
              )}

              {/* Meta */}
              <div className="flex items-center gap-3 flex-wrap mb-5">
                {post.category && (
                  <span
                    className="text-xs px-2.5 py-1 rounded-sm"
                    style={{ background: "var(--section-bg)", color: "var(--muted-ink)" }}
                  >
                    {post.category.name}
                  </span>
                )}
                <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                  <CalendarIcon size={11} />{toDateText(post.publishedAt)}
                </span>
                <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                  <ClockIcon size={11} />{post.readTime} 分钟阅读
                </span>
                <span className="flex items-center gap-1 text-xs" style={{ color: "var(--muted-ink)" }}>
                  <EyeIcon size={11} />{displayViewCount.toLocaleString()} 次阅读
                </span>
              </div>

              {/* Title */}
              <h1
                className="mb-8 leading-tight"
                style={{ fontFamily: "var(--fontDisplay)", fontSize: "clamp(28px,3.5vw,44px)", fontWeight: 400, color: "var(--ink)", lineHeight: 1.25 }}
              >
                {post.title}
              </h1>

              <ArticleAiSummary text={post.aiSummary ?? ""} />

              {/* Content */}
              <ArticleContent post={post} />

              <CopyrightNotice post={post} />

              {/* Tags */}
              {post.tags.length > 0 && (
                <div className="flex items-center gap-2 flex-wrap mt-12 pt-8 border-t" style={{ borderColor: "var(--warm-border)" }}>
                  <TagIcon size={14} style={{ color: "var(--muted-ink)" }} />
                  {post.tags.map((tag) => (
                    <Link
                      key={tag.id || tag.slug || tag.name}
                      to={`/tag/${encodeURIComponent(tag.slug || tag.name)}`}
                      className="text-xs px-3 py-1 rounded-full border hover:border-primary transition-colors"
                      style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)" }}
                    >
                      #{tag.name}
                    </Link>
                  ))}
                </div>
              )}

              {/* Related */}
              {related.length > 0 && (
                <div className="mt-14">
                  <h3 className="text-xs tracking-widest uppercase mb-6" style={{ color: "var(--muted-ink)" }}>相关文章</h3>
                  <div className="flex flex-col gap-0">
                    {related.map((item) => (
                      <Link
                        key={item.id}
                        to={`/posts/${item.slug}`}
                        className="flex gap-4 py-4 border-b hover:opacity-70 transition-opacity group flex-wrap"
                        style={{ borderColor: "var(--warm-border)" }}
                      >
                        {item.coverUrl && (
                          <div className="overflow-hidden shrink-0 w-20 h-14">
                            <img src={item.coverUrl.replace("w=1200", "w=200")} alt="" className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <div className="text-xs mb-1" style={{ color: "var(--muted-ink)" }}>{toDateText(item.publishedAt)}</div>
                          <div
                            className="leading-snug"
                            style={{ fontFamily: "var(--fontDisplay)", fontSize: "14px", color: "var(--ink)" }}
                          >
                            {item.title}
                          </div>
                        </div>
                      </Link>
                    ))}
                  </div>
                </div>
              )}

              <CommentSection targetType="article" targetKey={post.slug} />
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

              {/* TOC */}
              <ArticleToc toc={post.toc} />

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
          </>
        )}
      </article>
    </PageLayout>
  );
}
