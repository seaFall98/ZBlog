import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import ArticleToc from "./ArticleToc";

describe("ArticleToc", () => {
  it("renders real h2 and h3 anchors with Paico-style hierarchy", () => {
    const html = renderToStaticMarkup(
      <ArticleToc
        toc={[
          { id: "一-序言", title: "一、序言", level: 2 },
          { id: "引子", title: "引子", level: 3 },
        ]}
      />,
    );

    expect(html).toContain("目录");
    expect(html).toContain('href="#一-序言"');
    expect(html).toContain('href="#引子"');
    expect(html).toContain("article-toc__link--h3");
  });

  it("renders nothing when there is no TOC", () => {
    expect(renderToStaticMarkup(<ArticleToc toc={[]} />)).toBe("");
  });
});
