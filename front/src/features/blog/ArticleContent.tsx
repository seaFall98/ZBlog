import { Children, isValidElement, useEffect, useId, useRef } from "react";
import ReactMarkdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { headingId } from "./toc";
import type { PostView } from "./types";

function nextFallbackHeadingId(text: string, seen: Map<string, number>): string {
  const baseId = headingId(text);
  const count = seen.get(baseId) ?? 0;
  seen.set(baseId, count + 1);
  return count === 0 ? baseId : `${baseId}-${count + 1}`;
}

function languageFromClassName(className?: string): string {
  const match = /(?:^|\s)language-([^\s]+)/.exec(className ?? "");
  return match?.[1]?.toLowerCase() ?? "";
}

function textFromChildren(children: unknown): string {
  if (children === null || children === undefined || typeof children === "boolean") return "";
  if (typeof children === "string" || typeof children === "number") return String(children);
  if (Array.isArray(children)) return children.map(textFromChildren).join("");
  if (isValidElement<{ children?: unknown }>(children)) return textFromChildren(children.props.children);
  return "";
}

type MermaidDiagramProps = {
  chart: string;
};

function MermaidDiagram({ chart }: MermaidDiagramProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const reactId = useId().replace(/[^a-zA-Z0-9_-]/g, "");

  useEffect(() => {
    let cancelled = false;
    const element = containerRef.current;
    const source = chart.trim();

    if (!element || !source) return;

    element.textContent = source;

    async function renderDiagram() {
      try {
        const { default: mermaid } = await import("mermaid");
        mermaid.initialize({
          startOnLoad: false,
          theme: "default",
          securityLevel: "loose",
        });
        const { svg } = await mermaid.render(`mermaid-${reactId}`, source);
        if (!cancelled && element) {
          element.innerHTML = svg;
        }
      } catch (error) {
        console.error("Mermaid 渲染失败:", error);
        if (!cancelled && element) {
          element.classList.add("prose-blog__mermaid--error");
          element.textContent = source;
        }
      }
    }

    void renderDiagram();

    return () => {
      cancelled = true;
    };
  }, [chart, reactId]);

  return (
    <div ref={containerRef} className="prose-blog__mermaid" role="img" aria-label="Mermaid diagram">
      {chart}
    </div>
  );
}

type ArticleContentProps = {
  post: PostView;
};

export default function ArticleContent({ post }: ArticleContentProps) {
  if (!post.contentMarkdown) {
    return <article className="prose-blog" dangerouslySetInnerHTML={{ __html: post.contentHtml }} />;
  }

  let headingIndex = 0;
  const fallbackHeadingIds = new Map<string, number>();
  const components: Components = {
    h2({ children, node: _node, ...props }) {
      const text = String(children);
      const id = post.toc[headingIndex++]?.id ?? nextFallbackHeadingId(text, fallbackHeadingIds);
      return <h2 id={id} {...props}>{children}</h2>;
    },
    h3({ children, node: _node, ...props }) {
      const text = String(children);
      const id = post.toc[headingIndex++]?.id ?? nextFallbackHeadingId(text, fallbackHeadingIds);
      return <h3 id={id} {...props}>{children}</h3>;
    },
    pre({ children, node: _node, ...props }) {
      const codeChild = Children.toArray(children).find((child) => isValidElement<{ className?: string; children?: unknown }>(child));
      const language = codeChild ? languageFromClassName(codeChild.props.className) : "";

      if (language === "mermaid" && codeChild) {
        return <MermaidDiagram chart={textFromChildren(codeChild.props.children).replace(/\n$/, "")} />;
      }

      return <pre {...props}>{children}</pre>;
    },
    code({ className, children, node: _node, ...props }) {
      const language = languageFromClassName(className);
      return (
        <code className={className} data-language={language || undefined} {...props}>
          {children}
        </code>
      );
    },
  };

  return (
    <article className="prose-blog prose-blog--markdown">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {post.contentMarkdown}
      </ReactMarkdown>
    </article>
  );
}
