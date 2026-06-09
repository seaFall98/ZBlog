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

  it("maps existing backend basic profile keys", () => {
    expect(mapSafeSiteProfile({ site_name: "ZBlog", site_description: "慢慢记录", "basic.author_email": "me@example.com", "basic.author_avatar": "uploads/avatar.jpg" })).toMatchObject({
      title: "ZBlog",
      subtitle: "慢慢记录",
      email: "me@example.com",
      avatarUrl: "/uploads/avatar.jpg",
    });
  });
});

describe("mapMenus", () => {
  it("maps enabled menu tree", () => {
    expect(mapMenus([{ title: "写作", name: "写作", path: "/blog", enabled: true, children: [{ title: "分类", path: "/categories", enabled: true }] }])).toEqual([
      { label: "写作", href: "/blog", children: [{ label: "分类", href: "/categories", children: [] }] },
    ]);
  });

  it("normalizes legacy album menu URLs to gallery routes", () => {
    expect(mapMenus([{ title: "相册", url: "/album", enabled: true }])).toEqual([
      { label: "相册", href: "/gallery", children: [] },
    ]);
  });
});
