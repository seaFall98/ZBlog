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
      faviconUrl: "",
      established: "",
      heroEyebrow: "",
      heroTitle: "寂静之书",
      heroSlogan: "",
      footerDescription: "",
      footerCopyright: "",
      footerSlogan: "",
      backgroundImage: "",
      barrageBackgroundImage: "",
      messageContent: "",
      aboutDescribe: "记录生活",
      aboutDescribeTips: "",
      aboutExhibition: "",
      aboutProfile: "",
      aboutPersonality: "",
      aboutMottoMain: "",
      aboutMottoSub: "",
      aboutStory: "",
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

  it("maps prefixed blog public profile keys and normalizes media fields", () => {
    expect(mapSafeSiteProfile({
      "blog.title": "Z",
      "blog.subtitle": "慢写生活",
      "blog.slogan": "以文字作舟",
      "blog.description": "一个正在生长的博客",
      "blog.favicon": "uploads/favicon.ico",
      "blog.established": "2026",
      "blog.background_image": "uploads/bg.jpg",
      "blog.barrage_background_image": "/uploads/message.jpg",
      "blog.message_content": "欢迎留言",
      "blog.about_describe": "站长简介",
      "blog.about_profile": "[]",
      "blog.about_personality": "最近在读",
      "blog.about_motto_main": "[\"生活有光\"]",
      "blog.about_motto_sub": "一句话介绍",
      "blog.about_story": "时间轴",
      api_key: "secret",
      upload_secret: "secret",
      oauth_client_secret: "secret",
    })).toMatchObject({
      title: "Z",
      subtitle: "慢写生活",
      heroTitle: "Z",
      heroSlogan: "以文字作舟",
      footerDescription: "一个正在生长的博客",
      footerSlogan: "以文字作舟",
      faviconUrl: "/uploads/favicon.ico",
      established: "2026",
      backgroundImage: "/uploads/bg.jpg",
      barrageBackgroundImage: "/uploads/message.jpg",
      messageContent: "欢迎留言",
      aboutDescribe: "站长简介",
      aboutProfile: "[]",
      aboutPersonality: "最近在读",
      aboutMottoMain: "[\"生活有光\"]",
      aboutMottoSub: "一句话介绍",
      aboutStory: "时间轴",
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
