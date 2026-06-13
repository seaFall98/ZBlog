import { apiClient } from "../../lib/apiClient";
import type { FriendLinkView, FriendTypeView } from "./types";

type RawRecord = Record<string, unknown>;

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function mapFriend(value: unknown, category: string, typeId: number): FriendLinkView | null {
  if (!isRecord(value)) return null;
  const name = stringValue(value.name);
  const url = stringValue(value.url);
  if (!name || !url) return null;
  return {
    id: stringValue(value.id ?? url),
    name,
    url,
    description: stringValue(value.description),
    logo: stringValue(value.avatar ?? value.logo ?? value.screenshot),
    category: stringValue(value.type_name) || category || "友情链接",
    typeId: typeId || Number(stringValue(value.type_id)) || 0,
  };
}

export type FriendLinksResult = {
  links: FriendLinkView[];
  types: FriendTypeView[];
};

export async function fetchFriendLinks(): Promise<FriendLinksResult> {
  const data = await apiClient.get<RawRecord>("/friends");
  const groups = Array.isArray(data.groups) ? data.groups : [];

  const links: FriendLinkView[] = [];
  const types: FriendTypeView[] = [];

  for (const group of groups) {
    if (!isRecord(group)) continue;
    const typeId = Number(stringValue(group.type_id)) || 0;
    const typeName = stringValue(group.type_name);
    const category = typeName;

    if (typeId && typeName) {
      types.push({ id: typeId, name: typeName });
    }

    const friends = Array.isArray(group.friends) ? group.friends : [];
    for (const friend of friends) {
      const mapped = mapFriend(friend, category, typeId);
      if (mapped) links.push(mapped);
    }
  }

  return { links, types };
}
