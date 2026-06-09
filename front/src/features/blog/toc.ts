export type TocItem = {
  id: string;
  title: string;
  level: 2 | 3;
};

const headingMarker = /^(#{2,3})\s+(.+?)\s*#*\s*$/;

export function headingId(text: string): string {
  const slug = text
    .toLowerCase()
    .trim()
    .replace(/[#*_`~[\](){}:：，,。.、!！?？&/\\|]+/g, " ")
    .replace(/[^a-z0-9一-鿿]+/g, "-")
    .replace(/^-+|-+$/g, "");

  return slug || "section";
}

function uniqueId(baseId: string, seen: Map<string, number>): string {
  const count = seen.get(baseId) ?? 0;
  seen.set(baseId, count + 1);
  return count === 0 ? baseId : `${baseId}-${count + 1}`;
}

export function extractMarkdownToc(markdown: string): TocItem[] {
  const toc: TocItem[] = [];
  const seen = new Map<string, number>();
  let inFence = false;

  for (const line of markdown.split(/\r?\n/)) {
    if (/^\s*```/.test(line)) {
      inFence = !inFence;
      continue;
    }
    if (inFence) continue;

    const match = line.match(headingMarker);
    if (!match) continue;

    const level = match[1].length as 2 | 3;
    const title = match[2].trim();
    const id = uniqueId(headingId(title), seen);
    toc.push({ id, title, level });
  }

  return toc;
}
