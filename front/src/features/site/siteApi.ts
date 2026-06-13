import { apiClient } from "../../lib/apiClient";
import { normalizeMediaUrl } from "../../lib/mediaUrl";
import { isRecord, type RawRecord } from "../../lib/typeGuards";
import type {
  SiteMenuGroupsView,
  SiteMenuView,
  SiteProfileView,
  SiteSkillItemView,
  SiteSocialLinkView,
  SiteStatusItemView,
  SiteTimelineItemView,
} from "./types";

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value).trim();
}

function numberValue(value: unknown, fallback = 0): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function normalizeRemixIconName(value: unknown): string {
  const icon = stringValue(value);
  if (!icon) return "";
  return icon.startsWith("ri-") ? icon : `ri-${icon}`;
}

function sortByOrder<T extends { sort: number }>(list: T[]): T[] {
  return [...list].sort((left, right) => left.sort - right.sort);
}

function mapStatusItems(value: unknown): SiteStatusItemView[] {
  if (!Array.isArray(value)) return [];
  return sortByOrder(
    value
      .filter(isRecord)
      .map((item) => ({
        icon: stringValue(item.icon),
        label: stringValue(item.label),
        content: stringValue(item.content),
        sort: numberValue(item.sort, 0),
      }))
      .filter((item) => item.label && item.content),
  );
}

function mapSkillItems(value: unknown): SiteSkillItemView[] {
  if (!Array.isArray(value)) return [];
  return sortByOrder(
    value
      .filter(isRecord)
      .map((item) => ({
        name: stringValue(item.name),
        value: stringValue(item.value),
        sort: numberValue(item.sort, 0),
      }))
      .filter((item) => item.name && item.value),
  );
}

function mapTimelineItems(value: unknown): SiteTimelineItemView[] {
  if (!Array.isArray(value)) return [];
  return sortByOrder(
    value
      .filter(isRecord)
      .map((item) => ({
        year: stringValue(item.year),
        event: stringValue(item.event),
        sort: numberValue(item.sort, 0),
      }))
      .filter((item) => item.year && item.event),
  );
}

function mapSocialLinks(value: unknown): SiteSocialLinkView[] {
  if (!Array.isArray(value)) return [];
  return sortByOrder(
    value
      .filter(isRecord)
      .map((item) => ({
        icon: normalizeRemixIconName(item.icon),
        name: stringValue(item.name),
        url: stringValue(item.url),
        sort: numberValue(item.sort, 0),
      }))
      .filter((item) => item.name && item.url),
  );
}

export function mapFrontConfig(value: unknown): SiteProfileView {
  const record = isRecord(value) ? value : {};
  const identity = isRecord(record.identity) ? record.identity : {};
  const home = isRecord(record.home) ? record.home : {};
  const about = isRecord(record.about) ? record.about : {};
  const guestbook = isRecord(record.guestbook) ? record.guestbook : {};
  const footer = isRecord(record.footer) ? record.footer : {};

  return {
    title: stringValue(identity.siteTitle),
    ownerDisplayName: stringValue(identity.ownerDisplayName),
    subtitle: "",
    aboutIntro: stringValue(about.introText),
    email: stringValue(identity.email),
    avatarUrl: normalizeMediaUrl(stringValue(identity.primaryImageUrl)),
    faviconUrl: normalizeMediaUrl(stringValue(identity.faviconUrl)),
    established: "",
    icpRecord: stringValue(identity.icpRecord),
    policeRecord: stringValue(identity.policeRecord),
    heroEyebrow: stringValue(home.heroEyebrow),
    heroTitle: stringValue(home.heroTitle),
    heroMeta: stringValue(home.heroMeta),
    heroCtaLabel: stringValue(home.heroCtaLabel),
    heroCtaTarget: stringValue(home.heroCtaTarget),
    heroSlogan: stringValue(footer.slogan),
    footerDescription: stringValue(footer.description),
    footerCopyright: stringValue(footer.copyrightText),
    footerSlogan: stringValue(footer.slogan),
    socialLinks: mapSocialLinks(footer.socialLinks),
    backgroundImage: normalizeMediaUrl(stringValue(guestbook.backgroundImage)),
    barrageBackgroundImage: normalizeMediaUrl(stringValue(guestbook.backgroundImage)),
    guestbookIntro: stringValue(guestbook.introText),
    guestbookDanmakuLimit: typeof guestbook.danmakuPublicLimit === "number" ? guestbook.danmakuPublicLimit : 200,
    aboutStatusItems: mapStatusItems(about.statusItems),
    aboutSkillItems: mapSkillItems(about.skillItems),
    aboutTimelineItems: mapTimelineItems(about.timelineItems),
    aboutBottomQuote: stringValue(about.bottomQuote),
  };
}

function mapMenuNode(value: unknown): SiteMenuView | null {
  if (!isRecord(value)) return null;
  const label = stringValue(value.title || value.name || value.label);
  const href = stringValue(value.path || value.url || value.href || "/") || "/";
  const children = Array.isArray(value.children)
    ? value.children.map(mapMenuNode).filter((item): item is SiteMenuView => Boolean(item))
    : [];

  if (!label) return null;
  return { label, href, children };
}

export function mapMenuGroups(value: unknown): SiteMenuGroupsView {
  const record = isRecord(value) ? value : {};
  const header = Array.isArray(record.header)
    ? record.header.map(mapMenuNode).filter((item): item is SiteMenuView => Boolean(item))
    : [];
  const footer = Array.isArray(record.footer)
    ? record.footer.map(mapMenuNode).filter((item): item is SiteMenuView => Boolean(item))
    : [];

  return { header, footer };
}

export async function fetchSiteProfile(): Promise<SiteProfileView> {
  const data = await apiClient.get<unknown>("/front/config");
  return mapFrontConfig(data);
}

export async function fetchMenuGroups(): Promise<SiteMenuGroupsView> {
  const data = await apiClient.get<unknown>("/front/menus");
  return mapMenuGroups(data);
}
