import { describe, expect, it } from "vitest";
import { mapMenus, mapSafeSiteProfile } from "./siteApi";

describe("mapSafeSiteProfile", () => {
  it("uses only known safe keys", () => {
    expect(mapSafeSiteProfile({ site_title: "寂静之书", about_intro: "记录生活", mcp_secret: "secret" })).toEqual({
      title: "寂静之书",
      subtitle: "",
      aboutIntro: "记录生活",
      email: "",
      avatarUrl: "",
    });
  });
});

describe("mapMenus", () => {
  it("maps enabled menu tree", () => {
    expect(mapMenus([{ title: "写作", name: "写作", path: "/blog", enabled: true, children: [{ title: "分类", path: "/categories", enabled: true }] }])).toEqual([
      { label: "写作", href: "/blog", children: [{ label: "分类", href: "/categories", children: [] }] },
    ]);
  });
});
