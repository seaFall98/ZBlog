package com.zblog.event.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.event.application.port.ArticlePublishedNotifier;
import com.zblog.event.application.port.EventConsumptionRepository;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticlePublishedEventConsumer {

  // consumer name 是幂等消费维度，不是用户可见的通知类型。
  private static final String CONSUMER_NAME = "article-published-notification";

  private final EventConsumptionRepository eventConsumptionRepository;
  private final ObjectMapper objectMapper;
  private final ArticlePublishedNotifier articlePublishedNotifier;

  public ArticlePublishedEventConsumer(
      EventConsumptionRepository eventConsumptionRepository,
      ObjectMapper objectMapper,
      ArticlePublishedNotifier articlePublishedNotifier) {
    this.eventConsumptionRepository = eventConsumptionRepository;
    this.objectMapper = objectMapper;
    this.articlePublishedNotifier = articlePublishedNotifier;
  }

  @Transactional
  public void consume(long eventId, String eventType, String payload) {
    // broker 可能重复投递，同一事件在同一 consumer 维度只允许产生一次通知副作用。
    if (!"ARTICLE_PUBLISHED".equals(eventType) || eventConsumptionRepository.alreadyConsumed(eventId, CONSUMER_NAME)) {
      return;
    }
    Map<String, Object> data = readPayload(payload);
    long articleId = ((Number) data.get("article_id")).longValue();
    String title = data.get("title").toString();
    String slug = data.get("slug").toString();
    articlePublishedNotifier.notifyArticlePublished(articleId, title, slug, eventId);
    eventConsumptionRepository.markConsumed(eventId, CONSUMER_NAME);
  }

  private Map<String, Object> readPayload(String payload) {
    try {
      return objectMapper.readValue(payload, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid event payload", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
