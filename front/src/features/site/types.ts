export type SiteProfileView = {
  title: string;
  subtitle: string;
  aboutIntro: string;
  email: string;
  avatarUrl: string;
};

export type SiteMenuView = {
  label: string;
  href: string;
  children: SiteMenuView[];
};
