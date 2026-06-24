package com.zblog.scheduler;

import com.zblog.content.application.ArticleService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ArticleScheduledPublishJobHandler implements ScheduledJobHandler {

  private final ArticleService articleService;

  public ArticleScheduledPublishJobHandler(ArticleService articleService) {
    this.articleService = articleService;
  }

  public String name() {
    return "article-scheduled-publish";
  }

  public String description() {
    return "Publish draft articles whose scheduled publish time has arrived.";
  }

  public String execute(Map<String, Object> parameters) {
    int published = articleService.publishDueScheduledArticles();
    return "Published " + published + " scheduled articles.";
  }
}
