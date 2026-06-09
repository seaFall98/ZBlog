import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import ArticleContent from "./ArticleContent";
import type { PostView } from "./types";

function postWith(overrides: Partial<PostView> & Record<string, unknown>): PostView {
  return {
    id: "post-1",
    slug: "post-1",
    title: "文章",
    summary: "摘要",
    contentHtml: "<p>HTML fallback</p>",
    category: null,
    tags: [],
    publishedAt: "2024-10-24",
    coverUrl: "",
    readTime: 1,
    featured: false,
    source: "api",
    ...overrides,
  } as PostView;
}

describe("ArticleContent", () => {
  it("renders markdown with heading ids when contentMarkdown exists", () => {
    const html = renderToStaticMarkup(
      <ArticleContent
        post={postWith({
          contentMarkdown: "## 一、序言\n\n正文\n\n### 引子\n\n- GFM item",
          toc: [
            { id: "一-序言", title: "一、序言", level: 2 },
            { id: "引子", title: "引子", level: 3 },
          ],
        })}
      />,
    );

    expect(html).toContain('<h2 id="一-序言">一、序言</h2>');
    expect(html).toContain('<h3 id="引子">引子</h3>');
    expect(html).toContain("<li>GFM item</li>");
    expect(html).not.toContain("HTML fallback");
  });

  it("keeps existing HTML fallback when markdown is missing", () => {
    const html = renderToStaticMarkup(<ArticleContent post={postWith({ contentHtml: "<p>HTML fallback</p>" })} />);

    expect(html).toContain("<p>HTML fallback</p>");
  });

  it("keeps fallback heading ids unique when toc is shorter than rendered headings", () => {
    const html = renderToStaticMarkup(
      <ArticleContent
        post={postWith({
          contentMarkdown: "## 重复标题\n\n## 重复标题",
          toc: [],
        })}
      />,
    );

    expect(html).toContain('id="重复标题"');
    expect(html).toContain('id="重复标题-2"');
  });
});
