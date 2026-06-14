package com.zblog.event.infrastructure.messaging;

import com.zblog.event.EventProperties;
import com.zblog.event.application.port.EventMessageBroker;
import com.zblog.event.domain.OutboxEvent;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RabbitEventMessageBroker implements EventMessageBroker {

  // 生产消息路径按 event type 分 routing key，消费者只订阅自己关心的事件族。
  private final RabbitTemplate rabbitTemplate;
  private final EventProperties properties;

  public RabbitEventMessageBroker(RabbitTemplate rabbitTemplate, EventProperties properties) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  public void publish(OutboxEvent event) {
    rabbitTemplate.convertAndSend(
        properties.getExchange(),
        routingKey(event),
        Map.of("event_id", event.id(), "event_type", event.eventType(), "payload", event.payload()));
  }

  private String routingKey(OutboxEvent event) {
    if ("ARTICLE_SEARCH_UPSERT".equals(event.eventType()) || "ARTICLE_SEARCH_DELETE".equals(event.eventType())) {
      return properties.getSearchIndexRoutingKey();
    }
    if ("COMMENT_REPLY".equals(event.eventType())) {
      return properties.getCommentReplyRoutingKey();
    }
    return properties.getArticlePublishedRoutingKey();
  }
}
