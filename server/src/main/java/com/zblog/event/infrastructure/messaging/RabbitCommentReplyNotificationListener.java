package com.zblog.event.infrastructure.messaging;

import com.zblog.event.application.CommentReplyEventConsumer;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RabbitCommentReplyNotificationListener {

  private final CommentReplyEventConsumer consumer;

  public RabbitCommentReplyNotificationListener(CommentReplyEventConsumer consumer) {
    this.consumer = consumer;
  }

  @RabbitListener(queues = "${zblog.mq.comment-reply-queue:zblog.comment.reply}")
  public void handle(Map<String, Object> message) {
    long eventId = ((Number) message.get("event_id")).longValue();
    String eventType = message.get("event_type").toString();
    String payload = message.get("payload").toString();
    consumer.consume(eventId, eventType, payload);
  }
}
