import { describe, expect, it } from "vitest";
import { mapArticleToPostView } from "./blogMapper";

describe("mapArticleToPostView", () => {
  it("maps server article fields into PostView", () => {
    const post = mapArticleToPostView({
      id: 12,
      slug: "autumn-light",
      title: "秋日文章",
      summary: "一段摘要",
      content_html: "<p>后端 HTML</p>",
      category: { id: 3, slug: "life", name: "生活" },
      tags: [
        { id: 1, slug: "autumn", name: "秋日" },
        { id: 2, slug: "light", name: "光" },
      ],
      publish_time: "2024-10-24T12:30:00Z",
      cover_url: "https://example.com/cover.jpg",
      is_top: true,
    });

    expect(post).toMatchObject({
      id: "12",
      slug: "autumn-light",
      title: "秋日文章",
      summary: "一段摘要",
      contentHtml: "<p>后端 HTML</p>",
      category: { id: "3", slug: "life", name: "生活" },
      tags: [
        { id: "autumn", slug: "autumn", name: "秋日" },
        { id: "light", slug: "light", name: "光" },
      ],
      publishedAt: "2024-10-24T12:30:00Z",
      coverUrl: "https://example.com/cover.jpg",
      featured: true,
      source: "api",
    });
  });

  it("uses category url segment as slug when server category has no slug", () => {
    const post = mapArticleToPostView({
      id: 12,
      title: "生活文章",
      content_html: "<p>生活</p>",
      category: { id: 3, name: "生活", url: "/category/life" },
    });

    expect(post.category).toMatchObject({ id: "3", slug: "life", name: "生活" });
  });

  it("keeps object tag display name instead of numeric id when server tag has no slug", () => {
    const post = mapArticleToPostView({
      id: 12,
      title: "秋日文章",
      content_html: "<p>秋日</p>",
      tags: [{ id: 1, name: "秋日", url: "/tag/autumn" }],
    });

    expect(post.tags.map((tag) => tag.name)).toContain("秋日");
    expect(post.tags.map((tag) => tag.id)).toContain("autumn");
    expect(post.tags.map((tag) => tag.slug)).toContain("autumn");
    expect(post.tags.map((tag) => tag.id)).not.toContain("1");
    expect(post.tags.map((tag) => tag.slug)).not.toContain("1");
  });

  it("escapes markdown or text when HTML is missing", () => {
    const post = mapArticleToPostView({
      id: "unsafe",
      title: "无 HTML",
      content_markdown: "第一行\n<script>alert('x')</script>",
    });

    expect(post.contentHtml).toBe("<p>第一行<br />&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;</p>");
    expect(post.contentHtml).not.toContain("<script>");
  });

  it("preserves markdown content and extracts a real article TOC", () => {
    const post = mapArticleToPostView({
      id: "markdown-post",
      title: "Markdown 文章",
      content_html: "<p>后端 HTML fallback</p>",
      content_markdown: "## 一、序言\n\n正文\n\n### 引子\n\n```md\n## 代码标题\n```",
    });

    expect(post).toMatchObject({
      contentMarkdown: "## 一、序言\n\n正文\n\n### 引子\n\n```md\n## 代码标题\n```",
      toc: [
        { id: "一-序言", title: "一、序言", level: 2 },
        { id: "引子", title: "引子", level: 3 },
      ],
    });
  });
});
