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
    String confirmToken = subscriber.get("confirmation_token").toString();
    String unsubscribeToken = subscriber.get("unsubscribe_token").toString();
    mailOutboxService.send(
        "user",
        "subscribe_confirm",
        email,
        "请确认你的 ZBlog 订阅",
        "点击确认订阅：/api/v1/subscribe/confirm?token="
            + confirmToken
            + "\n如果不是你本人操作，可以忽略这封邮件。"
            + "\nunsubscribe token: "
            + unsubscribeToken);
  }

  public void sendUnsubscribeConfirm(Map<String, Object> subscriber) {
    String email = subscriber.get("email").toString();
    mailOutboxService.send("user", "unsubscribe_confirm", email, "已退订 ZBlog 更新 / 宸查€€璁?", "已退订：" + email + "\n宸查€€璁?");
  }

  public void sendArticlePublished(Map<String, Object> subscriber, Map<String, Object> article) {
    String email = subscriber.get("email").toString();
    String title = article.get("title").toString();
    String slug = article.get("slug").toString();
    String token = subscriber.get("unsubscribe_token").toString();
    mailOutboxService.send(
        "subscriber",
        "article_published",
        email,
        "新文章发布：" + title,
        title + "\n阅读地址：/posts/" + slug + "\n退订：/api/v1/subscribe/unsubscribe?token=" + token);
  }
}
