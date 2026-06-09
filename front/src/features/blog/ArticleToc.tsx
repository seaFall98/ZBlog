import type { TocItem } from "./toc";

type ArticleTocProps = {
  toc: TocItem[];
};

export default function ArticleToc({ toc }: ArticleTocProps) {
  if (toc.length === 0) return null;

  return (
    <nav className="article-toc" aria-label="文章目录">
      <div className="article-toc__title">目录</div>
      <div className="article-toc__items">
        {toc.map((item) => (
          <a
            key={item.id}
            className={`article-toc__link article-toc__link--h${item.level}`}
            href={`#${item.id}`}
          >
            {item.level === 3 ? "· " : ""}{item.title}
          </a>
        ))}
      </div>
    </nav>
  );
}
