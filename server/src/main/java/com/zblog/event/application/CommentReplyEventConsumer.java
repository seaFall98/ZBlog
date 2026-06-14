package com.zblog.event.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.event.application.port.EventConsumptionRepository;
import com.zblog.notification.NotificationService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentReplyEventConsumer {

  private static final String CONSUMER_NAME = "comment-reply-notification";

  private final EventConsumptionRepository eventConsumptionRepository;
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  public CommentReplyEventConsumer(
      EventConsumptionRepository eventConsumptionRepository,
      ObjectMapper objectMapper,
      NotificationService notificationService) {
    this.eventConsumptionRepository = eventConsumptionRepository;
    this.objectMapper = objectMapper;
    this.notificationService = notificationService;
  }

  @Transactional
  public void consume(long eventId, String eventType, String payload) {
    if (!"COMMENT_REPLY".equals(eventType) || eventConsumptionRepository.alreadyConsumed(eventId, CONSUMER_NAME)) {
      return;
    }
    Map<String, Object> data = readPayload(payload);
    notificationService.createCommentReplyNotification(
        number(data, "recipient_user_id"),
        number(data, "actor_user_id"),
        text(data, "actor_nickname"),
        text(data, "target_type"),
        text(data, "target_key"),
        number(data, "comment_id"),
        number(data, "parent_id"),
        text(data, "content"));
    eventConsumptionRepository.markConsumed(eventId, CONSUMER_NAME);
  }

  private Map<String, Object> readPayload(String payload) {
    try {
      return objectMapper.readValue(payload, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid event payload", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private long number(Map<String, Object> data, String key) {
    return ((Number) data.get(key)).longValue();
  }

  private String text(Map<String, Object> data, String key) {
    Object value = data.get(key);
    return value == null ? "" : value.toString();
  }
}
