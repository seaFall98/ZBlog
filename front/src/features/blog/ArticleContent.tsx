import type { PostView } from "./types";

type ArticleContentProps = {
  post: PostView;
};

export default function ArticleContent({ post }: ArticleContentProps) {
  return <article className="prose-blog" dangerouslySetInnerHTML={{ __html: post.contentHtml }} />;
}
