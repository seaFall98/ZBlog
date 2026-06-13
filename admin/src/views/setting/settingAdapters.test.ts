import { describe, expect, it } from "vitest";
import {
  buildV2AboutPayload,
  buildV2FooterPayload,
  buildV2GuestbookPayload,
  buildV2HomePayload,
  buildV2IdentityPayload,
  mapV2BlogSettingsToForm,
  mapV2IdentitySettingsToForm,
} from "./settingAdapters";

describe("settingAdapters", () => {
  it("maps v2 identity settings into the basic settings form", () => {
    expect(
      mapV2IdentitySettingsToForm({
        site_title: "寂静之书",
        owner_display_name: "seaFall98",
        email: "hello@example.com",
        primary_image_url: "/uploads/about.jpg",
        favicon_url: "/uploads/favicon.png",
        icp_record: "沪 ICP 12345678 号",
        police_record: "沪公网安备 123456789 号",
      }),
    ).toEqual({
      site_title: "寂静之书",
      owner_display_name: "seaFall98",
      email: "hello@example.com",
      primary_image_url: "/uploads/about.jpg",
      favicon_url: "/uploads/favicon.png",
      icp_record: "沪 ICP 12345678 号",
      police_record: "沪公网安备 123456789 号",
    });
  });

  it("maps v2 home about guestbook and footer settings into the blog form", () => {
    expect(
      mapV2BlogSettingsToForm({
        home: {
          hero_eyebrow: "个人出版物",
          hero_title: "以文字作舟",
          hero_meta: "12 篇文章",
          hero_cta_label: "阅读文章",
          hero_cta_target: "/blog",
        },
        about: {
          intro_text: "顶部介绍",
          status_items:
            '[{"icon":"ri-book-line","label":"正在读","content":"百年孤独","sort":1}]',
          skill_items: '[{"name":"写作","value":"90","sort":1}]',
          timeline_items: '[{"year":"2024","event":"开始认真记录","sort":1}]',
          bottom_quote: "生活就是很多很多个平凡的日子，偶尔有一些光。",
        },
        guestbook: {
          intro_text: "把想说的话留在这里。",
          background_image: "/uploads/guestbook-cover.jpg",
          danmaku_public_limit: "200",
        },
        search: { hot_keywords: "旅行,摄影" },
        footer: {
          description: "页脚描述",
          copyright_text: "© 2026",
          slogan: "以文字作舟，渡光阴之河",
          social_links:
            '[{"icon":"ri-github-line","name":"GitHub","url":"https://github.com/example","sort":1}]',
        },
      }),
    ).toEqual({
      hero_eyebrow: "个人出版物",
      hero_title: "以文字作舟",
      hero_meta: "12 篇文章",
      hero_cta_label: "阅读文章",
      hero_cta_target: "/blog",
      about_intro_text: "顶部介绍",
      about_status_items: [
        {
          icon: "ri-book-line",
          label: "正在读",
          content: "百年孤独",
          sort: 1,
        },
      ],
      about_skill_items: [{ name: "写作", value: "90", sort: 1 }],
      about_timeline_items: [
        { year: "2024", event: "开始认真记录", sort: 1 },
      ],
      about_bottom_quote: "生活就是很多很多个平凡的日子，偶尔有一些光。",
      guestbook_intro_text: "把想说的话留在这里。",
      guestbook_background_image: "/uploads/guestbook-cover.jpg",
      guestbook_danmaku_limit: "200",
      search_hot_keywords: "旅行,摄影",
      footer_description: "页脚描述",
      footer_copyright_text: "© 2026",
      footer_slogan: "以文字作舟，渡光阴之河",
      footer_social_links: [
        {
          icon: "ri-github-line",
          name: "GitHub",
          url: "https://github.com/example",
          sort: 1,
        },
      ],
    });
  });

  it("builds v2 payloads from the admin forms", () => {
    const identityForm = {
      site_title: "寂静之书",
      owner_display_name: "seaFall98",
      email: "hello@example.com",
      primary_image_url: "/uploads/about.jpg",
      favicon_url: "/uploads/favicon.png",
      icp_record: "沪 ICP 12345678 号",
      police_record: "沪公网安备 123456789 号",
    };

    const blogForm = {
      hero_eyebrow: "个人出版物",
      hero_title: "以文字作舟",
      hero_meta: "",
      hero_cta_label: "阅读文章",
      hero_cta_target: "/blog",
      about_intro_text: "顶部介绍",
      about_status_items: [
        {
          icon: "ri-book-line",
          label: "正在读",
          content: "百年孤独",
          sort: 1,
        },
      ],
      about_skill_items: [{ name: "写作", value: "90", sort: 1 }],
      about_timeline_items: [
        { year: "2024", event: "开始认真记录", sort: 1 },
      ],
      about_bottom_quote: "生活就是很多很多个平凡的日子，偶尔有一些光。",
      guestbook_intro_text: "把想说的话留在这里。",
      guestbook_background_image: "/uploads/guestbook-cover.jpg",
      guestbook_danmaku_limit: "200",
      search_hot_keywords: "旅行,摄影",
      footer_description: "页脚描述",
      footer_copyright_text: "© 2026",
      footer_slogan: "以文字作舟，渡光阴之河",
      footer_social_links: [
        {
          icon: "ri-github-line",
          name: "GitHub",
          url: "https://github.com/example",
          sort: 1,
        },
      ],
    };

    expect(buildV2IdentityPayload(identityForm)).toEqual({
      site_title: "寂静之书",
      owner_display_name: "seaFall98",
      email: "hello@example.com",
      primary_image_url: "/uploads/about.jpg",
      favicon_url: "/uploads/favicon.png",
      icp_record: "沪 ICP 12345678 号",
      police_record: "沪公网安备 123456789 号",
    });

    expect(buildV2HomePayload(blogForm)).toEqual({
      hero_eyebrow: "个人出版物",
      hero_title: "以文字作舟",
      hero_meta: "",
      hero_cta_label: "阅读文章",
      hero_cta_target: "/blog",
    });

    expect(buildV2AboutPayload(blogForm)).toEqual({
      intro_text: "顶部介绍",
      status_items:
        '[{"icon":"ri-book-line","label":"正在读","content":"百年孤独","sort":1}]',
      skill_items: '[{"name":"写作","value":"90","sort":1}]',
      timeline_items: '[{"year":"2024","event":"开始认真记录","sort":1}]',
      bottom_quote: "生活就是很多很多个平凡的日子，偶尔有一些光。",
    });

    expect(buildV2GuestbookPayload(blogForm)).toEqual({
      intro_text: "把想说的话留在这里。",
      background_image: "/uploads/guestbook-cover.jpg",
      danmaku_public_limit: "200",
    });

    expect(buildV2FooterPayload(blogForm)).toEqual({
      description: "页脚描述",
      copyright_text: "© 2026",
      slogan: "以文字作舟，渡光阴之河",
      social_links:
        '[{"icon":"ri-github-line","name":"GitHub","url":"https://github.com/example","sort":1}]',
    });
  });
});
