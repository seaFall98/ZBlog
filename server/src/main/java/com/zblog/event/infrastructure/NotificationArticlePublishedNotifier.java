package com.zblog.event.infrastructure;

import com.zblog.event.application.port.ArticlePublishedNotifier;
import com.zblog.notification.NotificationService;
import com.zblog.subscription.application.SubscriptionService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationArticlePublishedNotifier implements ArticlePublishedNotifier {

  private final NotificationService notificationService;
  private final SubscriptionService subscriptionService;

  public NotificationArticlePublishedNotifier(NotificationService notificationService, SubscriptionService subscriptionService) {
    this.notificationService = notificationService;
    this.subscriptionService = subscriptionService;
  }

  public void notifyArticlePublished(long articleId, String title, String slug, long eventId) {
    notificationService.createArticlePublishedNotification(articleId, title, slug, eventId);
    subscriptionService.enqueueArticlePublished(Map.of("id", articleId, "title", title, "slug", slug));
  }
}
