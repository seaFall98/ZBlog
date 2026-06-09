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

export function mapSafeSiteProfile(value: unknown): SiteProfileView {
  const record = flattenSettings(value);
  return {
    title: stringValue(record.site_title ?? record.title),
    subtitle: stringValue(record.site_subtitle ?? record.subtitle),
    aboutIntro: stringValue(record.about_intro ?? record.about),
    email: stringValue(record.contact_email ?? record.email),
    avatarUrl: normalizeMediaUrl(stringValue(record.avatar_url ?? record.avatar)),
  };
}

export function mapMenus(value: unknown): SiteMenuView[] {
  const list = Array.isArray(value) ? value : isRecord(value) && Array.isArray(value.list) ? value.list : [];
  return list
    .filter(isRecord)
    .filter((item) => item.enabled !== false && item.visible !== false && item.status !== "DISABLED")
    .map((item) => ({
      label: stringValue(item.title ?? item.name ?? item.label),
      href: stringValue(item.path ?? item.url ?? item.href) || "/",
      children: mapMenus(item.children),
    }))
    .filter((item) => item.label && item.href);
}

export async function fetchSiteProfile(): Promise<SiteProfileView> {
  const data = await apiClient.get<RawRecord>("/settings/basic");
  return mapSafeSiteProfile(data);
}

export async function fetchMenus(): Promise<SiteMenuView[]> {
  const data = await apiClient.get<unknown>("/menus");
  return mapMenus(data);
}
