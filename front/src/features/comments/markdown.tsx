export function CommentMarkdown({ content }: { content: string }) {
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
          <p
            key={`${index}-${text.slice(0, 12)}`}
            className={quote ? "border-l-2 pl-3 text-sm leading-7" : "text-sm leading-7"}
            style={{
              borderColor: "var(--warm-border)",
              color: quote ? "var(--muted-ink)" : "var(--ink)",
              fontFamily: "var(--fontBody)",
            }}
          >
            {text}
          </p>
        );
      })}
    </div>
  );
}
