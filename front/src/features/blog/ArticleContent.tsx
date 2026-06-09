import ReactMarkdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { headingId } from "./toc";
import type { PostView } from "./types";

type ArticleContentProps = {
  post: PostView;
};

export default function ArticleContent({ post }: ArticleContentProps) {
  if (!post.contentMarkdown) {
    return <article className="prose-blog" dangerouslySetInnerHTML={{ __html: post.contentHtml }} />;
  }

  let headingIndex = 0;
  const components: Components = {
    h2({ children, node: _node, ...props }) {
      const id = post.toc[headingIndex++]?.id ?? headingId(String(children));
      return <h2 id={id} {...props}>{children}</h2>;
    },
    h3({ children, node: _node, ...props }) {
      const id = post.toc[headingIndex++]?.id ?? headingId(String(children));
      return <h3 id={id} {...props}>{children}</h3>;
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
