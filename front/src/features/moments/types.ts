export type MomentLinkView = {
  url: string;
  title: string;
  favicon?: string;
};

export type MusicLinkView = {
  title: string;
  artist?: string;
  cover?: string;
  url: string;
};

export type VideoSourceView = {
  url: string;
  platform?: string;
  videoId?: string;
};

export type MomentView = {
  id: string;
  text: string;
  images: string[];
  date: string;
  mood: string;
  tags: string[];
  location: string;
  link: MomentLinkView | null;
  video?: VideoSourceView;
  audio?: string;
  music?: MusicLinkView | null;
};
