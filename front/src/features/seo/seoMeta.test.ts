import { describe, expect, it } from "vitest";
import { buildArticleJsonLd, buildArticleSeo, buildPageSeo } from "./seoMeta";

describe("seoMeta", () => {
  it("builds concise page metadata from site and route names", () => {
    const seo = buildPageSeo({
      siteTitle: "寂静之书",
      siteDescription: "记录平凡生活里的光与影，写作是一种安静的对话。",
      pageTitle: "文章",
      path: "/blog",
      origin: "https://blog.example.test",
    });

    expect(seo).toMatchObject({
      title: "文章｜寂静之书",
      description: "记录平凡生活里的光与影，写作是一种安静的对话。",
      canonicalUrl: "https://blog.example.test/blog",
    });
    expect(seo.description).not.toContain("Paico");
    expect(seo.description.length).toBeLessThanOrEqual(120);
  });

  it("builds article OG and Twitter metadata from real article fields", () => {
    const seo = buildArticleSeo({
      siteTitle: "寂静之书",
      origin: "https://blog.example.test",
      article: {
        title: "秋日午后的光线总是来得比预想中更温柔",
        summary: "窗外的梧桐叶还挂着，金黄中带一点锈色。",
        slug: "autumn-light",
        coverUrl: "/uploads/autumn.jpg",
        publishedAt: "2024-10-24T12:30:00Z",
        updatedAt: "2024-10-25T12:30:00Z",
      },
    });

    expect(seo.title).toBe("秋日午后的光线总是来得比预想中更温柔｜寂静之书");
    expect(seo.description).toBe("窗外的梧桐叶还挂着，金黄中带一点锈色。");
    expect(seo.canonicalUrl).toBe("https://blog.example.test/posts/autumn-light");
    expect(seo.openGraph).toMatchObject({
      type: "article",
      title: "秋日午后的光线总是来得比预想中更温柔",
      image: "https://blog.example.test/uploads/autumn.jpg",
      url: "https://blog.example.test/posts/autumn-light",
    });
    expect(seo.twitter).toMatchObject({
      card: "summary_large_image",
      image: "https://blog.example.test/uploads/autumn.jpg",
    });
  });

  it("builds article JSON-LD without UI-only filler fields", () => {
    const jsonLd = buildArticleJsonLd({
      siteTitle: "寂静之书",
      origin: "https://blog.example.test",
      article: {
        title: "慢读：重新学习用眼睛触碰文字",
        summary: "我们已经太习惯快速阅读了。",
        slug: "on-reading-slowly",
        coverUrl: "https://cdn.example.test/cover.jpg",
        publishedAt: "2024-10-18T08:00:00Z",
        updatedAt: "2024-10-19T08:00:00Z",
      },
    });

    expect(jsonLd).toMatchObject({
      "@context": "https://schema.org",
      "@type": "Article",
      headline: "慢读：重新学习用眼睛触碰文字",
      description: "我们已经太习惯快速阅读了。",
      image: ["https://cdn.example.test/cover.jpg"],
      datePublished: "2024-10-18T08:00:00Z",
      dateModified: "2024-10-19T08:00:00Z",
      mainEntityOfPage: "https://blog.example.test/posts/on-reading-slowly",
    });
    expect(JSON.stringify(jsonLd)).not.toContain("Paico");
  });
});
