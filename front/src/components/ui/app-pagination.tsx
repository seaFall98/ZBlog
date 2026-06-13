import { ChevronLeftIcon, ChevronRightIcon, MoreHorizontalIcon } from "lucide-react";

type AppPaginationProps = {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
};

function visiblePages(page: number, totalPages: number): Array<number | "ellipsis"> {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const pages = new Set([1, totalPages, page - 1, page, page + 1]);
  const ordered = Array.from(pages)
    .filter((item) => item >= 1 && item <= totalPages)
    .sort((left, right) => left - right);

  return ordered.flatMap((item, index) => {
    const previous = ordered[index - 1];
    return previous && item - previous > 1 ? ["ellipsis" as const, item] : [item];
  });
}

export function AppPagination({ page, totalPages, onPageChange, className = "" }: AppPaginationProps) {
  const safeTotalPages = Math.max(0, Math.floor(totalPages));
  if (safeTotalPages <= 1 && page <= 1) return null;

  const displayPage = safeTotalPages > 0 ? Math.min(Math.max(1, page), safeTotalPages) : Math.max(1, page);
  const canGoPrev = displayPage > 1;
  const canGoNext = safeTotalPages > 0 && displayPage < safeTotalPages;
  const pages = safeTotalPages > 0 ? visiblePages(displayPage, safeTotalPages) : [displayPage];

  return (
    <nav className={`mt-12 flex items-center justify-center gap-2 ${className}`} aria-label="分页">
      <button
        type="button"
        onClick={() => canGoPrev && onPageChange(displayPage - 1)}
        disabled={!canGoPrev}
        className="inline-flex h-9 min-w-9 items-center justify-center border px-3 text-xs transition-opacity disabled:cursor-not-allowed disabled:opacity-35"
        style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)", background: "var(--warm-white)" }}
        aria-label="上一页"
      >
        <ChevronLeftIcon size={14} />
      </button>

      {pages.map((item, index) =>
        item === "ellipsis" ? (
          <span
            key={`ellipsis-${index}`}
            className="inline-flex h-9 min-w-9 items-center justify-center text-xs"
            style={{ color: "var(--muted-ink)" }}
          >
            <MoreHorizontalIcon size={14} />
          </span>
        ) : (
          <button
            key={item}
            type="button"
            onClick={() => onPageChange(item)}
            aria-current={item === displayPage ? "page" : undefined}
            className="inline-flex h-9 min-w-9 items-center justify-center border px-3 text-xs transition-opacity hover:opacity-70"
            style={{
              borderColor: item === displayPage ? "var(--ink)" : "var(--warm-border)",
              background: item === displayPage ? "var(--ink)" : "var(--warm-white)",
              color: item === displayPage ? "var(--warm-white)" : "var(--muted-ink)",
            }}
          >
            {item}
          </button>
        ),
      )}

      <button
        type="button"
        onClick={() => canGoNext && onPageChange(displayPage + 1)}
        disabled={!canGoNext}
        className="inline-flex h-9 min-w-9 items-center justify-center border px-3 text-xs transition-opacity disabled:cursor-not-allowed disabled:opacity-35"
        style={{ borderColor: "var(--warm-border)", color: "var(--muted-ink)", background: "var(--warm-white)" }}
        aria-label="下一页"
      >
        <ChevronRightIcon size={14} />
      </button>
    </nav>
  );
}
