import { describe, expect, it } from "vitest";
import { mapFrontConfig, mapMenuGroups } from "./siteApi";

describe("mapFrontConfig", () => {
  it("maps the v2 front config payload into the site profile view", () => {
    expect(
      mapFrontConfig({
        identity: {
          siteTitle: "Quiet Book",
          ownerDisplayName: "Sea",
          email: "hello@example.com",
          primaryImageUrl: "uploads/avatar.jpg",
          faviconUrl: "uploads/favicon.ico",
          icpRecord: "ICP 123",
          policeRecord: "Police 456",
        },
        home: {
          heroEyebrow: "Personal Edition",
          heroTitle: "Use words as the boat",
          heroMeta: "Latest updates",
          heroCtaLabel: "Read Posts",
          heroCtaTarget: "/blog",
        },
        about: {
          introText: "A quiet corner on the internet.",
          statusItems: [
            { icon: "book-open", label: "Reading", content: "One Hundred Years", sort: 2 },
            { icon: "camera", label: "Shooting", content: "Autumn streets", sort: 1 },
          ],
          skillItems: [
            { name: "Writing", value: "90", sort: 2 },
            { name: "Photography", value: "75", sort: 1 },
          ],
          timelineItems: [
            { year: "2024", event: "Started weekly updates", sort: 2 },
            { year: "2023", event: "Took the first solo trip", sort: 1 },
          ],
          bottomQuote: "Life is made of ordinary days with a little light.",
        },
        guestbook: {
          introText: "Leave a few words here.",
          backgroundImage: "uploads/guestbook-cover.jpg",
        },
        footer: {
          description: "A growing blog.",
          copyrightText: "© 2026 Quiet Book",
          slogan: "Use words as the boat",
          socialLinks: [
            { icon: "github-line", name: "GitHub", url: "https://github.com/example", sort: 2 },
            { icon: "mail-line", name: "Email", url: "mailto:hello@example.com", sort: 1 },
          ],
        },
      }),
    ).toEqual({
      title: "Quiet Book",
      ownerDisplayName: "Sea",
      subtitle: "",
      aboutIntro: "A quiet corner on the internet.",
      email: "hello@example.com",
      avatarUrl: "/uploads/avatar.jpg",
      faviconUrl: "/uploads/favicon.ico",
      established: "",
      icpRecord: "ICP 123",
      policeRecord: "Police 456",
      heroEyebrow: "Personal Edition",
      heroTitle: "Use words as the boat",
      heroMeta: "Latest updates",
      heroCtaLabel: "Read Posts",
      heroCtaTarget: "/blog",
      heroSlogan: "Use words as the boat",
      footerDescription: "A growing blog.",
      footerCopyright: "© 2026 Quiet Book",
      footerSlogan: "Use words as the boat",
      socialLinks: [
        { icon: "ri-mail-line", name: "Email", url: "mailto:hello@example.com", sort: 1 },
        { icon: "ri-github-line", name: "GitHub", url: "https://github.com/example", sort: 2 },
      ],
      backgroundImage: "/uploads/guestbook-cover.jpg",
      barrageBackgroundImage: "/uploads/guestbook-cover.jpg",
      guestbookIntro: "Leave a few words here.",
      aboutStatusItems: [
        { icon: "camera", label: "Shooting", content: "Autumn streets", sort: 1 },
        { icon: "book-open", label: "Reading", content: "One Hundred Years", sort: 2 },
      ],
      aboutSkillItems: [
        { name: "Photography", value: "75", sort: 1 },
        { name: "Writing", value: "90", sort: 2 },
      ],
      aboutTimelineItems: [
        { year: "2023", event: "Took the first solo trip", sort: 1 },
        { year: "2024", event: "Started weekly updates", sort: 2 },
      ],
      aboutBottomQuote: "Life is made of ordinary days with a little light.",
    });
  });

  it("ignores malformed nested values", () => {
    expect(mapFrontConfig({ identity: "bad", footer: null })).toMatchObject({
      title: "",
      ownerDisplayName: "",
      footerDescription: "",
      socialLinks: [],
      aboutStatusItems: [],
      aboutSkillItems: [],
      aboutTimelineItems: [],
    });
  });
});

describe("mapMenuGroups", () => {
  it("maps the v2 menu trees into header and footer groups", () => {
    expect(
      mapMenuGroups({
        header: [
          {
            title: "Writing",
            url: "/blog",
            children: [{ title: "Categories", url: "/categories" }],
          },
        ],
        footer: [
          {
            title: "Protocols",
            children: [{ title: "Privacy Policy", url: "/privacy-policy" }],
          },
        ],
      }),
    ).toEqual({
      header: [
        {
          label: "Writing",
          href: "/blog",
          children: [{ label: "Categories", href: "/categories", children: [] }],
        },
      ],
      footer: [
        {
          label: "Protocols",
          href: "/",
          children: [{ label: "Privacy Policy", href: "/privacy-policy", children: [] }],
        },
      ],
    });
  });

  it("drops invalid menu nodes", () => {
    expect(mapMenuGroups({ header: [{ url: "/blog" }], footer: [{ title: "", url: "/x" }] })).toEqual({
      header: [],
      footer: [],
    });
  });
});
