package com.zblog.event.infrastructure.messaging;

import com.zblog.event.application.ArticlePublishedEventConsumer;
import com.zblog.event.application.port.EventMessageBroker;
import com.zblog.event.domain.OutboxEvent;
import com.zblog.search.infrastructure.messaging.SearchIndexEventConsumer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryEventMessageBroker implements EventMessageBroker {

  // 测试 broker 同步 fan-out，模拟事件广播但不依赖 RabbitMQ。
  private final ArticlePublishedEventConsumer consumer;
  private final SearchIndexEventConsumer searchIndexEventConsumer;

  public InMemoryEventMessageBroker(
      ArticlePublishedEventConsumer consumer, SearchIndexEventConsumer searchIndexEventConsumer) {
    this.consumer = consumer;
    this.searchIndexEventConsumer = searchIndexEventConsumer;
  }

  public void publish(OutboxEvent event) {
    consumer.consume(event.id(), event.eventType(), event.payload());
    searchIndexEventConsumer.consume(event.id(), event.eventType(), event.payload());
  }
}
