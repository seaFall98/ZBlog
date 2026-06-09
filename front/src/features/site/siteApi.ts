import { apiClient } from "../../lib/apiClient";
import { normalizeMediaUrl } from "../../lib/mediaUrl";
import type { SiteMenuView, SiteProfileView } from "./types";

type RawRecord = Record<string, unknown>;

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function flattenSettings(value: unknown): RawRecord {
  if (!isRecord(value)) return {};
  const record: RawRecord = { ...value };
  const list = Array.isArray(value.list) ? value.list : Array.isArray(value.items) ? value.items : [];
  list.filter(isRecord).forEach((item) => {
    const key = stringValue(item.key ?? item.name ?? item.setting_key);
    if (key) record[key] = item.value ?? item.setting_value ?? item.content;
  });
  return record;
}

function pick(record: RawRecord, ...keys: string[]): string {
  for (const key of keys) {
    const value = stringValue(record[key]).trim();
    if (value) return value;
  }
  return "";
}

export function mapSafeSiteProfile(value: unknown): SiteProfileView {
  const record = flattenSettings(value);
  const title = pick(record, "title", "blog.title", "site_title", "site_name");
  const subtitle = pick(record, "subtitle", "blog.subtitle", "site_subtitle", "site_description");
  const slogan = pick(record, "slogan", "blog.slogan");
  const description = pick(record, "description", "blog.description");
  const aboutIntro = pick(record, "aboutIntro", "about_intro", "aboutDescribe", "about_describe", "blog.about_describe", "basic.author_desc", "author_desc", "about");
  const footerDescription = pick(record, "footerDescription", "footer_description", "blog.footer_description", "description", "blog.description", "subtitle", "blog.subtitle");
  return {
    title,
    subtitle,
    aboutIntro,
    email: pick(record, "email", "contact_email", "basic.author_email", "author_email"),
    avatarUrl: normalizeMediaUrl(pick(record, "avatarUrl", "avatar_url", "basic.author_avatar", "author_avatar", "avatar")),
    faviconUrl: normalizeMediaUrl(pick(record, "faviconUrl", "favicon_url", "favicon", "blog.favicon")),
    established: pick(record, "established", "blog.established"),
    heroEyebrow: pick(record, "heroEyebrow", "hero_eyebrow", "blog.hero_eyebrow"),
    heroTitle: pick(record, "heroTitle", "hero_title", "blog.hero_title", "title", "blog.title", "site_title", "site_name"),
    heroSlogan: pick(record, "heroSlogan", "hero_slogan", "blog.hero_slogan", "slogan", "blog.slogan"),
    footerDescription,
    footerCopyright: pick(record, "footerCopyright", "footer_copyright", "blog.footer_copyright"),
    footerSlogan: pick(record, "footerSlogan", "footer_slogan", "blog.footer_slogan", "slogan", "blog.slogan"),
    backgroundImage: normalizeMediaUrl(pick(record, "backgroundImage", "background_image", "blog.background_image")),
    barrageBackgroundImage: normalizeMediaUrl(pick(record, "barrageBackgroundImage", "barrage_background_image", "blog.barrage_background_image")),
    messageContent: pick(record, "messageContent", "message_content", "blog.message_content"),
    aboutDescribe: aboutIntro,
    aboutDescribeTips: pick(record, "aboutDescribeTips", "about_describe_tips", "blog.about_describe_tips"),
    aboutExhibition: normalizeMediaUrl(pick(record, "aboutExhibition", "about_exhibition", "blog.about_exhibition")),
    aboutProfile: pick(record, "aboutProfile", "about_profile", "blog.about_profile"),
    aboutPersonality: pick(record, "aboutPersonality", "about_personality", "blog.about_personality"),
    aboutMottoMain: pick(record, "aboutMottoMain", "about_motto_main", "blog.about_motto_main"),
    aboutMottoSub: pick(record, "aboutMottoSub", "about_motto_sub", "blog.about_motto_sub"),
    aboutStory: pick(record, "aboutStory", "about_story", "blog.about_story"),
  };
}

function normalizeMenuHref(value: unknown): string {
  const href = stringValue(value).trim() || "/";
  return href === "/album" ? "/gallery" : href;
}

export function mapMenus(value: unknown): SiteMenuView[] {
  const list = Array.isArray(value) ? value : isRecord(value) && Array.isArray(value.list) ? value.list : [];
  return list
    .filter(isRecord)
    .filter((item) => item.enabled !== false && item.visible !== false && item.status !== "DISABLED")
    .map((item) => ({
      label: stringValue(item.title ?? item.name ?? item.label),
      href: normalizeMenuHref(item.path ?? item.url ?? item.href),
      children: mapMenus(item.children),
    }))
    .filter((item) => item.label && item.href);
}

export async function fetchSiteProfile(): Promise<SiteProfileView> {
  const data = await apiClient.get<RawRecord>("/settings/public-profile");
  return mapSafeSiteProfile(data);
}

export async function fetchMenus(): Promise<SiteMenuView[]> {
  const data = await apiClient.get<unknown>("/menus");
  return mapMenus(data);
}
