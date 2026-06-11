import { renderToStaticMarkup } from "react-dom/server";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Footer from "./Footer";
import { useSiteProfile } from "../../features/site/useSiteProfile";

vi.mock("../../features/site/useSiteProfile", () => ({
  useSiteProfile: vi.fn(),
}));

const mockedUseSiteProfile = vi.mocked(useSiteProfile);

describe("Footer", () => {
  beforeEach(() => {
    mockedUseSiteProfile.mockReturnValue({
      profile: {
        title: "寂静之书",
        ownerDisplayName: "SeaFall",
        subtitle: "",
        aboutIntro: "",
        email: "",
        avatarUrl: "",
        faviconUrl: "",
        established: "2024",
        icpRecord: "",
        policeRecord: "",
        heroEyebrow: "",
        heroTitle: "",
        heroMeta: "",
        heroCtaLabel: "",
        heroCtaTarget: "",
        heroSlogan: "",
        footerDescription: "记录平凡生活里的光与影，写作是一种安静的对话。",
        footerCopyright: "© 2024 寂静之书",
        footerSlogan: "以文字作舟，渡光阴之河",
        socialLinks: [
          {
            icon: "github-line",
            name: "GitHub",
            url: "https://github.com/example",
            sort: 1,
          },
          {
            icon: "mail-line",
            name: "邮箱",
            url: "mailto:test@example.com",
            sort: 2,
          },
        ],
        backgroundImage: "",
        barrageBackgroundImage: "",
        guestbookIntro: "",
        aboutStatusItems: [],
        aboutSkillItems: [],
        aboutTimelineItems: [],
        aboutBottomQuote: "",
      },
      headerMenus: [],
      footerMenus: [
        {
          label: "导航",
          href: "/",
          children: [{ label: "首页", href: "/", children: [] }],
        },
      ],
      loading: false,
      loaded: true,
      error: null,
    });
  });

  it("renders social links inside the bottom bar center slot instead of a standalone top row", () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    );

    const bottomIndex = html.indexOf('data-slot="footer-bottom"');
    const socialIndex = html.indexOf('data-slot="footer-social"');

    expect(bottomIndex).toBeGreaterThan(-1);
    expect(socialIndex).toBeGreaterThan(bottomIndex);
    expect(html).toContain("md:absolute md:left-1/2 md:top-1/2");
    expect(html).not.toContain("mb-10 flex flex-wrap items-center gap-3");
  });
});
