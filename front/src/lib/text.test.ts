import { describe, expect, it } from "vitest";
import { escapeHtml, estimateReadTime, slugFromText, stripHtml, toDateText, truncateText } from "./text";

describe("text helpers", () => {
  it("escapes HTML special characters", () => {
    expect(escapeHtml("<script>alert('x')</script>")).toBe("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;");
  });

  it("strips html tags", () => {
    expect(stripHtml("<p>秋日午后的光线</p>")).toBe("秋日午后的光线");
  });

  it("truncates long text", () => {
    expect(truncateText("一二三四五", 3)).toBe("一二三…");
  });

  it("estimates read time with minimum one minute", () => {
    expect(estimateReadTime("寂静之书")).toBe(1);
  });

  it("counts CJK characters and non-CJK words together for mixed read time", () => {
    const englishWords = Array.from({ length: 401 }, (_, index) => `word${index}`).join(" ");

    expect(estimateReadTime(`${englishWords} 中`)).toBe(3);
  });

  it("formats date-like text", () => {
    expect(toDateText("2026-06-07T08:00:00Z")).toBe("2026-06-07");
  });

  it("formats UTC instants with the project local calendar date", () => {
    expect(toDateText("2026-06-06T16:30:00Z")).toBe("2026-06-07");
  });

  it("keeps the input calendar date for ISO-like strings with timezone offsets", () => {
    expect(toDateText("2026-06-07T00:30:00+08:00")).toBe("2026-06-07");
  });

  it("creates readable slug fallback", () => {
    expect(slugFromText("Autumn Light!")).toBe("autumn-light");
  });
});
