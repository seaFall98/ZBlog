import type { ReactNode } from "react";

export function CommentMarkdown({ content }: { content: string }) {
  const imagePattern = /!\[([^\]]*)]\(([^)\s]+)(?:\s+"[^"]*")?\)/g;

  function renderInline(block: string) {
    const nodes: ReactNode[] = [];
    let lastIndex = 0;
    for (const match of block.matchAll(imagePattern)) {
      const index = match.index ?? 0;
      if (index > lastIndex) {
        nodes.push(block.slice(lastIndex, index));
      }
      nodes.push(
        <img
          key={`${match[2]}-${index}`}
          src={match[2]}
          alt={match[1] || ""}
          loading="lazy"
          className="mt-2 max-h-64 max-w-full border object-contain"
          style={{ borderColor: "var(--warm-border)" }}
        />,
      );
      lastIndex = index + match[0].length;
    }
    if (lastIndex < block.length) {
      nodes.push(block.slice(lastIndex));
    }
    return nodes;
  }

  const blocks = content
    .split(/\n{2,}/)
    .map((block) => block.trim())
    .filter(Boolean);

  return (
    <div className="space-y-3">
      {blocks.map((block, index) => {
        const quote = block.startsWith(">");
        const text = quote ? block.replace(/^>\s?/, "") : block;
        return (
          <div
            key={`${index}-${text.slice(0, 12)}`}
            className={quote ? "border-l-2 pl-3 text-sm leading-7" : "text-sm leading-7"}
            style={{
              borderColor: "var(--warm-border)",
              color: quote ? "var(--muted-ink)" : "var(--ink)",
              fontFamily: "var(--fontBody)",
            }}
          >
            {renderInline(text)}
          </div>
        );
      })}
    </div>
  );
}
