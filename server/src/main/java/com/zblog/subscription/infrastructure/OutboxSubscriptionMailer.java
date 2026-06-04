package com.zblog.subscription.infrastructure;

import com.zblog.mail.MailOutboxService;
import com.zblog.subscription.application.port.SubscriptionMailer;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OutboxSubscriptionMailer implements SubscriptionMailer {

  private final MailOutboxService mailOutboxService;

  public OutboxSubscriptionMailer(MailOutboxService mailOutboxService) {
    this.mailOutboxService = mailOutboxService;
  }

  public void sendSubscribeConfirm(Map<String, Object> subscriber) {
    String email = subscriber.get("email").toString();
    String token = subscriber.get("unsubscribe_token").toString();
    mailOutboxService.send(
        "user",
        "subscribe_confirm",
        email,
        "Subscribe confirm",
        "Subscription confirmed. unsubscribe token: " + token);
  }

  public void sendUnsubscribeConfirm(Map<String, Object> subscriber) {
    mailOutboxService.send(
        "user",
        "unsubscribe_confirm",
        subscriber.get("email").toString(),
        "已退订",
        "已退订 " + subscriber.get("email"));
  }
}
