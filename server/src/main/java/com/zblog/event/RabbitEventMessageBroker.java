package com.zblog.event;

import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RabbitEventMessageBroker implements EventMessageBroker {

  private final RabbitTemplate rabbitTemplate;
  private final EventProperties properties;

  public RabbitEventMessageBroker(RabbitTemplate rabbitTemplate, EventProperties properties) {
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  @Override
  public void publish(OutboxEvent event) {
    rabbitTemplate.convertAndSend(
        properties.getExchange(),
        properties.getArticlePublishedRoutingKey(),
        Map.of("event_id", event.id(), "event_type", event.eventType(), "payload", event.payload()));
  }
}
