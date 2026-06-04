package com.zblog.content.infrastructure;

import com.zblog.content.application.port.ArticleEventPublisher;
import com.zblog.content.domain.ArticleSearchProjection;
import com.zblog.event.application.EventOutboxService;
import com.zblog.search.domain.SearchDocument;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OutboxArticleEventPublisher implements ArticleEventPublisher {

  // 这是 content 到 event outbox 的桥接 adapter，不承载普通文章业务规则。
  private final EventOutboxService eventOutboxService;

  public OutboxArticleEventPublisher(EventOutboxService eventOutboxService) {
    this.eventOutboxService = eventOutboxService;
  }

  public void articlePublished(Map<String, Object> article) {
    eventOutboxService.createArticlePublishedEvent(article);
  }

  public void articleSearchUpsert(ArticleSearchProjection projection) {
    // 搜索 payload 在 adapter 边界组装，避免搜索文档契约反向污染 content application。
    eventOutboxService.createArticleSearchUpsertEvent(
        new SearchDocument(
            projection.articleId(),
            projection.title(),
            projection.slug(),
            projection.summary(),
            projection.contentText(),
            projection.publishedAt()));
  }

  public void articleSearchDelete(long articleId) {
    eventOutboxService.createArticleSearchDeleteEvent(articleId);
  }
}
