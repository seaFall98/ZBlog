export type SiteStatusItemView = {
  icon: string;
  label: string;
  content: string;
  sort: number;
};

export type SiteSkillItemView = {
  name: string;
  value: string;
  sort: number;
};

export type SiteTimelineItemView = {
  year: string;
  event: string;
  sort: number;
};

export type SiteSocialLinkView = {
  icon: string;
  name: string;
  url: string;
  sort: number;
};

export type SiteProfileView = {
  title: string;
  ownerDisplayName: string;
  subtitle: string;
  aboutIntro: string;
  email: string;
  avatarUrl: string;
  faviconUrl: string;
  established: string;
  icpRecord: string;
  policeRecord: string;
  heroEyebrow: string;
  heroTitle: string;
  heroMeta: string;
  heroCtaLabel: string;
  heroCtaTarget: string;
  heroSlogan: string;
  footerDescription: string;
  footerCopyright: string;
  footerSlogan: string;
  socialLinks: SiteSocialLinkView[];
  backgroundImage: string;
  barrageBackgroundImage: string;
  guestbookIntro: string;
  guestbookDanmakuLimit: number;
  aboutStatusItems: SiteStatusItemView[];
  aboutSkillItems: SiteSkillItemView[];
  aboutTimelineItems: SiteTimelineItemView[];
  aboutBottomQuote: string;
};

export type SiteMenuView = {
  label: string;
  href: string;
  children: SiteMenuView[];
};

export type SiteMenuGroupsView = {
  header: SiteMenuView[];
  footer: SiteMenuView[];
};
