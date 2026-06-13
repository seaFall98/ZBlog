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

export type MomentView = {
  id: string;
  text: string;
  images: string[];
  date: string;
  mood: string;
  tags: string[];
  location: string;
  link: MomentLinkView | null;
  video?: string;
  audio?: string;
  music?: MusicLinkView | null;
};
