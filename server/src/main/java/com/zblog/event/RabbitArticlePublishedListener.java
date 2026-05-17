package com.zblog.event;

import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RabbitArticlePublishedListener {

  private final ArticlePublishedEventConsumer consumer;

  public RabbitArticlePublishedListener(ArticlePublishedEventConsumer consumer) {
    this.consumer = consumer;
  }

  @RabbitListener(queues = "${zblog.mq.article-published-queue:zblog.article.published}")
  public void handle(Map<String, Object> message) {
    long eventId = ((Number) message.get("event_id")).longValue();
    String eventType = message.get("event_type").toString();
    String payload = message.get("payload").toString();
    consumer.consume(eventId, eventType, payload);
  }
}
