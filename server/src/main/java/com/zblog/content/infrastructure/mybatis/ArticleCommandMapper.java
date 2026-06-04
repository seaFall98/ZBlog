package com.zblog.content.infrastructure.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleCommandMapper {

  int insertArticle(ArticleCommandInsertCommand command);

  int updateArticle(ArticleCommandUpdateCommand command);

  int publishArticle(@Param("id") long id);

  int unpublishArticle(@Param("id") long id);

  int deleteArticle(@Param("id") long id);

  int deleteTags(@Param("articleId") long articleId);

  int insertTag(@Param("articleId") long articleId, @Param("tagId") long tagId);

  class ArticleCommandInsertCommand {
    private Long id;
    private final String title;
    private final String slug;
    private final String markdown;
    private final String html;
    private final String text;
    private final String summary;
    private final String cover;
    private final Long categoryId;
    private final String location;
    private final boolean top;
    private final boolean essence;
    private final boolean outdated;

    ArticleCommandInsertCommand(
        String title,
        String slug,
        String markdown,
        String html,
        String text,
        String summary,
        String cover,
        Long categoryId,
        String location,
        boolean top,
        boolean essence,
        boolean outdated) {
      this.title = title;
      this.slug = slug;
      this.markdown = markdown;
      this.html = html;
      this.text = text;
      this.summary = summary;
      this.cover = cover;
      this.categoryId = categoryId;
      this.location = location;
      this.top = top;
      this.essence = essence;
      this.outdated = outdated;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getTitle() {
      return title;
    }

    public String getSlug() {
      return slug;
    }

    public String getMarkdown() {
      return markdown;
    }

    public String getHtml() {
      return html;
    }

    public String getText() {
      return text;
    }

    public String getSummary() {
      return summary;
    }

    public String getCover() {
      return cover;
    }

    public Long getCategoryId() {
      return categoryId;
    }

    public String getLocation() {
      return location;
    }

    public boolean isTop() {
      return top;
    }

    public boolean isEssence() {
      return essence;
    }

    public boolean isOutdated() {
      return outdated;
    }
  }

  record ArticleCommandUpdateCommand(
      long id,
      String title,
      String slug,
      String markdown,
      String html,
      String text,
      String summary,
      String cover,
      Long categoryId,
      String location,
      boolean top,
      boolean essence,
      boolean outdated) {}
}
