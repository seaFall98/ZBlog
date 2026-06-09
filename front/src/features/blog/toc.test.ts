import { describe, expect, it } from "vitest";
import { extractMarkdownToc, headingId } from "./toc";

describe("Markdown TOC", () => {
  it("extracts h2 and h3 headings while ignoring fenced code blocks", () => {
    const markdown = [
      "# 页面标题",
      "## 一、序言",
      "### 引子",
      "```ts",
      "## 代码里的标题",
      "### 代码里的小标题",
      "```",
      "~~~ts",
      "## 波浪线代码里的标题",
      "~~~",
      "#### 太深的标题",
      "## 一、序言",
    ].join("\n");

    expect(extractMarkdownToc(markdown)).toEqual([
      { id: "一-序言", title: "一、序言", level: 2 },
      { id: "引子", title: "引子", level: 3 },
      { id: "一-序言-2", title: "一、序言", level: 2 },
    ]);
  });

  it("generates stable ids from markdown heading text", () => {
    expect(headingId("秋日 Light & Shadow")).toBe("秋日-light-shadow");
    expect(headingId("   ###   ")).toBe("section");
  });

  it("keeps language names ending with # in heading titles", () => {
    expect(extractMarkdownToc("## C#\n\n## F# ###")).toEqual([
      { id: "c", title: "C#", level: 2 },
      { id: "f", title: "F#", level: 2 },
    ]);
  });
});
