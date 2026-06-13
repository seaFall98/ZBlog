export type BasicSettingsForm = {
  site_title: string;
  owner_display_name: string;
  email: string;
  primary_image_url: string;
  favicon_url: string;
  icp_record: string;
  police_record: string;
};

export type StatusItemForm = {
  icon: string;
  label: string;
  content: string;
  sort: number;
};

export type SkillItemForm = {
  name: string;
  value: string;
  sort: number;
};

export type TimelineItemForm = {
  year: string;
  event: string;
  sort: number;
};

export type SocialLinkForm = {
  icon: string;
  name: string;
  url: string;
  sort: number;
};

export type BlogSettingsForm = {
  hero_eyebrow: string;
  hero_title: string;
  hero_meta: string;
  hero_cta_label: string;
  hero_cta_target: string;
  about_intro_text: string;
  about_status_items: StatusItemForm[];
  about_skill_items: SkillItemForm[];
  about_timeline_items: TimelineItemForm[];
  about_bottom_quote: string;
  guestbook_intro_text: string;
  guestbook_background_image: string;
  guestbook_danmaku_limit: string;
  search_hot_keywords: string;
  footer_description: string;
  footer_copyright_text: string;
  footer_slogan: string;
  footer_social_links: SocialLinkForm[];
};

type FlatSettings = Record<string, string>;

type BlogSettingsGroups = {
  home: FlatSettings;
  about: FlatSettings;
  guestbook: FlatSettings;
  footer: FlatSettings;
  search: FlatSettings;
};

export function createDefaultBasicSettingsForm(): BasicSettingsForm {
  return {
    site_title: "",
    owner_display_name: "",
    email: "",
    primary_image_url: "",
    favicon_url: "",
    icp_record: "",
    police_record: "",
  };
}

export function createDefaultBlogSettingsForm(): BlogSettingsForm {
  return {
    hero_eyebrow: "",
    hero_title: "",
    hero_meta: "",
    hero_cta_label: "",
    hero_cta_target: "",
    about_intro_text: "",
    about_status_items: [],
    about_skill_items: [],
    about_timeline_items: [],
    about_bottom_quote: "",
    guestbook_intro_text: "",
    guestbook_background_image: "",
    guestbook_danmaku_limit: "200",
    search_hot_keywords: "",
    footer_description: "",
    footer_copyright_text: "",
    footer_slogan: "",
    footer_social_links: [],
  };
}

export function mapV2IdentitySettingsToForm(settings: FlatSettings): BasicSettingsForm {
  return {
    site_title: read(settings, "site_title"),
    owner_display_name: read(settings, "owner_display_name"),
    email: read(settings, "email"),
    primary_image_url: read(settings, "primary_image_url"),
    favicon_url: read(settings, "favicon_url"),
    icp_record: read(settings, "icp_record"),
    police_record: read(settings, "police_record"),
  };
}

export function mapV2BlogSettingsToForm(groups: BlogSettingsGroups): BlogSettingsForm {
  return {
    hero_eyebrow: read(groups.home, "hero_eyebrow"),
    hero_title: read(groups.home, "hero_title"),
    hero_meta: read(groups.home, "hero_meta"),
    hero_cta_label: read(groups.home, "hero_cta_label"),
    hero_cta_target: read(groups.home, "hero_cta_target"),
    about_intro_text: read(groups.about, "intro_text"),
    about_status_items: parseList<StatusItemForm>(read(groups.about, "status_items")),
    about_skill_items: parseList<SkillItemForm>(read(groups.about, "skill_items")),
    about_timeline_items: parseList<TimelineItemForm>(read(groups.about, "timeline_items")),
    about_bottom_quote: read(groups.about, "bottom_quote"),
    guestbook_intro_text: read(groups.guestbook, "intro_text"),
    guestbook_background_image: read(groups.guestbook, "background_image"),
    guestbook_danmaku_limit: read(groups.guestbook, "danmaku_public_limit") || "200",
    search_hot_keywords: read(groups.search, "hot_keywords"),
    footer_description: read(groups.footer, "description"),
    footer_copyright_text: read(groups.footer, "copyright_text"),
    footer_slogan: read(groups.footer, "slogan"),
    footer_social_links: parseList<SocialLinkForm>(read(groups.footer, "social_links")),
  };
}

export function buildV2IdentityPayload(form: BasicSettingsForm): FlatSettings {
  return {
    site_title: form.site_title,
    owner_display_name: form.owner_display_name,
    email: form.email,
    primary_image_url: form.primary_image_url,
    favicon_url: form.favicon_url,
    icp_record: form.icp_record,
    police_record: form.police_record,
  };
}

export function buildV2HomePayload(form: BlogSettingsForm): FlatSettings {
  return {
    hero_eyebrow: form.hero_eyebrow,
    hero_title: form.hero_title,
    hero_meta: form.hero_meta,
    hero_cta_label: form.hero_cta_label,
    hero_cta_target: form.hero_cta_target,
  };
}

export function buildV2AboutPayload(form: BlogSettingsForm): FlatSettings {
  return {
    intro_text: form.about_intro_text,
    status_items: JSON.stringify(form.about_status_items),
    skill_items: JSON.stringify(form.about_skill_items),
    timeline_items: JSON.stringify(form.about_timeline_items),
    bottom_quote: form.about_bottom_quote,
  };
}

export function buildV2GuestbookPayload(form: BlogSettingsForm): FlatSettings {
  return {
    intro_text: form.guestbook_intro_text,
    background_image: form.guestbook_background_image,
    danmaku_public_limit: form.guestbook_danmaku_limit,
  };
}

export function buildV2SearchPayload(form: BlogSettingsForm): FlatSettings {
  return {
    hot_keywords: form.search_hot_keywords,
  };
}

export function buildV2FooterPayload(form: BlogSettingsForm): FlatSettings {
  return {
    description: form.footer_description,
    copyright_text: form.footer_copyright_text,
    slogan: form.footer_slogan,
    social_links: JSON.stringify(form.footer_social_links),
  };
}

function read(settings: FlatSettings, key: string): string {
  return settings[key] ?? "";
}

function parseList<T>(raw: string): T[] {
  if (!raw.trim()) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as unknown;
    return Array.isArray(parsed) ? (parsed as T[]) : [];
  } catch {
    return [];
  }
}
