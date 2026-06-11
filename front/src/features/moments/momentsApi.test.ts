import { describe, expect, it } from "vitest";
import { mapMoment } from "./momentsApi";

describe("mapMoment", () => {
  it("uses explicit mood when present", () => {
    expect(mapMoment({ id: 1, content: { text: "咖啡很好", mood: "满足" }, publish_time: "2024-10-20T10:00:00Z" })).toMatchObject({
      mood: "满足",
      tags: [],
    });
  });

  it("falls back to first tag instead of always using 平静", () => {
    expect(mapMoment({ id: 2, content: { text: "散步", tags: "满足,散步" } })).toMatchObject({
      mood: "满足",
      tags: ["满足", "散步"],
    });
  });

  it("maps images, link and location without exposing raw JSON", () => {
    expect(mapMoment({ id: 3, content: { text: "看到一篇文章", images: ["uploads/a.jpg"], link: { url: "https://example.com", title: "文章" }, location: "杭州" } })).toMatchObject({
      images: ["/uploads/a.jpg"],
      link: { url: "https://example.com", title: "文章" },
      location: "杭州",
    });
  });
});
