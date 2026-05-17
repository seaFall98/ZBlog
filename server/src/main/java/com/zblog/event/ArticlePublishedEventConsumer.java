package com.zblog.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.notification.NotificationService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticlePublishedEventConsumer {

  private static final String CONSUMER_NAME = "article-published-notification";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  public ArticlePublishedEventConsumer(
      JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, NotificationService notificationService) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.notificationService = notificationService;
  }

  @Transactional
  public void consume(long eventId, String eventType, String payload) {
    if (!"ARTICLE_PUBLISHED".equals(eventType) || alreadyConsumed(eventId)) {
      return;
    }
    Map<String, Object> data = readPayload(payload);
    long articleId = ((Number) data.get("article_id")).longValue();
    String title = data.get("title").toString();
    String slug = data.get("slug").toString();
    notificationService.createArticlePublishedNotification(articleId, title, slug, eventId);
    jdbcTemplate.update(
        "insert into event_consumptions (event_id, consumer_name) values (?, ?)", eventId, CONSUMER_NAME);
  }

  private boolean alreadyConsumed(long eventId) {
    Long count =
        jdbcTemplate.queryForObject(
            "select count(*) from event_consumptions where event_id = ? and consumer_name = ?",
            Long.class,
            eventId,
            CONSUMER_NAME);
    return count != null && count > 0;
  }

  private Map<String, Object> readPayload(String payload) {
    try {
      return objectMapper.readValue(payload, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid event payload", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
