package com.zblog.search.infrastructure.messaging;

import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RabbitSearchIndexListener {

  private final SearchIndexEventConsumer consumer;

  public RabbitSearchIndexListener(SearchIndexEventConsumer consumer) {
    this.consumer = consumer;
  }

  @RabbitListener(queues = "${zblog.mq.search-index-queue:zblog.search.index}")
  public void handle(Map<String, Object> message) {
    long eventId = ((Number) message.get("event_id")).longValue();
    String eventType = message.get("event_type").toString();
    String payload = message.get("payload").toString();
    consumer.consume(eventId, eventType, payload);
  }
}
