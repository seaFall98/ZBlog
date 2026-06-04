package com.zblog.event.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.event.application.port.EventMessageBroker;
import com.zblog.event.application.port.EventOutboxRepository;
import com.zblog.event.domain.OutboxEvent;
import com.zblog.search.domain.SearchDocument;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EventOutboxService {

  // Outbox 先记录跨模块事实，再由 scheduler/broker 投递，降低业务事务与 MQ 的耦合。
  private final EventOutboxRepository eventOutboxRepository;
  private final ObjectMapper objectMapper;
  private final EventMessageBroker broker;

  public EventOutboxService(
      EventOutboxRepository eventOutboxRepository, ObjectMapper objectMapper, EventMessageBroker broker) {
    this.eventOutboxRepository = eventOutboxRepository;
    this.objectMapper = objectMapper;
    this.broker = broker;
  }

  public void createArticlePublishedEvent(Map<String, Object> article) {
    long articleId = ((Number) article.get("id")).longValue();
    Map<String, Object> payload =
        Map.of(
            "article_id", articleId,
            "title", article.get("title"),
            "slug", article.get("slug"));
    eventOutboxRepository.createArticleEvent("ARTICLE_PUBLISHED", articleId, writeJson(payload));
  }

  public void createArticleSearchUpsertEvent(SearchDocument document) {
    // 事件 payload 是 content 与 search 的跨模块 contract，字段变更需同步消费者和测试。
    Map<String, Object> payload =
        Map.of(
            "article_id", document.articleId(),
            "title", document.title(),
            "slug", document.slug(),
            "summary", document.summary() == null ? "" : document.summary(),
            "content_text", document.contentText() == null ? "" : document.contentText(),
            "published_at", document.publishedAt() == null ? "" : document.publishedAt());
    eventOutboxRepository.createArticleEvent("ARTICLE_SEARCH_UPSERT", document.articleId(), writeJson(payload));
  }

  public void createArticleSearchDeleteEvent(long articleId) {
    eventOutboxRepository.createArticleEvent(
        "ARTICLE_SEARCH_DELETE", articleId, writeJson(Map.of("article_id", articleId)));
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize, String status) {
    return eventOutboxRepository.list(page, pageSize, status);
  }

  public Map<String, Object> publishPending() {
    var events = eventOutboxRepository.pendingForPublish();
    int published = 0;
    int failed = 0;
    for (OutboxEvent event : events) {
      // 单条事件失败只标记 failed，不阻断本轮其它待投递事件。
      eventOutboxRepository.markProcessing(event.id());
      try {
        broker.publish(event);
        eventOutboxRepository.markSent(event.id());
        published++;
      } catch (RuntimeException exception) {
        eventOutboxRepository.markFailed(event.id(), exception.getMessage());
        failed++;
      }
    }
    return Map.of("published", published, "failed", failed, "total", events.size());
  }

  private String writeJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid event payload", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
