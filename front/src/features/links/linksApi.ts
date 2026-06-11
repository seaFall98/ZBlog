import { apiClient } from "../../lib/apiClient";
import type { FriendLinkView } from "./types";

type RawRecord = Record<string, unknown>;

function isRecord(value: unknown): value is RawRecord {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function stringValue(value: unknown): string {
  return value === undefined || value === null ? "" : String(value);
}

function mapFriend(value: unknown, category: string): FriendLinkView | null {
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
  };
}

export async function fetchFriendLinks(): Promise<FriendLinkView[]> {
  const data = await apiClient.get<RawRecord>("/friends");
  const groups = Array.isArray(data.groups) ? data.groups : [];
  return groups.flatMap((group) => {
    if (!isRecord(group)) return [];
    const category = stringValue(group.type_name);
    const friends = Array.isArray(group.friends) ? group.friends : [];
    return friends.map((friend) => mapFriend(friend, category)).filter((friend): friend is FriendLinkView => Boolean(friend));
  });
}
