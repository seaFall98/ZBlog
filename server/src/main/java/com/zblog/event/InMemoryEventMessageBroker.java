package com.zblog.event;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class InMemoryEventMessageBroker implements EventMessageBroker {

  private final ArticlePublishedEventConsumer consumer;

  public InMemoryEventMessageBroker(ArticlePublishedEventConsumer consumer) {
    this.consumer = consumer;
  }

  @Override
  public void publish(OutboxEvent event) {
    consumer.consume(event.id(), event.eventType(), event.payload());
  }
}
