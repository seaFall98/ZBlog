package com.zblog.content.infrastructure.mybatis;

import com.zblog.content.application.port.ArticleAdminQueryRepository;
import com.zblog.content.application.port.ArticleCommandRepository;
import com.zblog.content.infrastructure.mybatis.ArticleCommandMapper.ArticleCommandInsertCommand;
import com.zblog.content.infrastructure.mybatis.ArticleCommandMapper.ArticleCommandUpdateCommand;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisArticleCommandRepository implements ArticleCommandRepository {

  private final ArticleCommandMapper articleCommandMapper;
  private final ArticleAdminQueryRepository adminQueryRepository;

  public MyBatisArticleCommandRepository(
      ArticleCommandMapper articleCommandMapper, ArticleAdminQueryRepository adminQueryRepository) {
    this.articleCommandMapper = articleCommandMapper;
    this.adminQueryRepository = adminQueryRepository;
  }

  public Map<String, Object> create(
      String title,
      String slug,
      String markdown,
      String html,
      String text,
      String summary,
      String cover,
      Long categoryId,
      List<Long> tagIds,
      String location,
      boolean top,
      boolean essence,
      boolean outdated) {
    ArticleCommandInsertCommand command =
        new ArticleCommandInsertCommand(
            title,
            slug,
            markdown,
            html,
            text,
            summary,
            cover,
            categoryId,
            location,
            top,
            essence,
            outdated);
    articleCommandMapper.insertArticle(command);
    long id = generatedId(command);
    replaceTags(id, tagIds);
    return adminQueryRepository.getAdmin(id);
  }

  public Map<String, Object> update(
      long id,
      String title,
      String slug,
      String markdown,
      String html,
      String text,
      String summary,
      String cover,
      Long categoryId,
      List<Long> tagIds,
      String location,
      boolean top,
      boolean essence,
      boolean outdated) {
    articleCommandMapper.updateArticle(
        new ArticleCommandUpdateCommand(
            id,
            title,
            slug,
            markdown,
            html,
            text,
            summary,
            cover,
            categoryId,
            location,
            top,
            essence,
            outdated));
    replaceTags(id, tagIds);
    return adminQueryRepository.getAdmin(id);
  }

  public Map<String, Object> publish(long id) {
    articleCommandMapper.publishArticle(id);
    return adminQueryRepository.getAdmin(id);
  }

  public Map<String, Object> unpublish(long id) {
    articleCommandMapper.unpublishArticle(id);
    return adminQueryRepository.getAdmin(id);
  }

  public void delete(long id) {
    articleCommandMapper.deleteArticle(id);
  }

  private void replaceTags(long articleId, List<Long> tagIds) {
    articleCommandMapper.deleteTags(articleId);
    for (Long tagId : tagIds == null ? List.<Long>of() : tagIds) {
      articleCommandMapper.insertTag(articleId, tagId);
    }
  }

  private long generatedId(ArticleCommandInsertCommand command) {
    if (command.getId() == null) {
      throw new IllegalStateException("Article id was not generated");
    }
    return command.getId();
  }
}
