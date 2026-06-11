export type SiteProfileView = {
  title: string;
  subtitle: string;
  aboutIntro: string;
  email: string;
  avatarUrl: string;
  faviconUrl: string;
  established: string;
  heroEyebrow: string;
  heroTitle: string;
  heroSlogan: string;
  footerDescription: string;
  footerCopyright: string;
  footerSlogan: string;
  backgroundImage: string;
  barrageBackgroundImage: string;
  messageContent: string;
  aboutDescribe: string;
  aboutDescribeTips: string;
  aboutExhibition: string;
  aboutProfile: string;
  aboutPersonality: string;
  aboutMottoMain: string;
  aboutMottoSub: string;
  aboutStory: string;
};

export type SiteMenuView = {
  label: string;
  href: string;
  children: SiteMenuView[];
};
