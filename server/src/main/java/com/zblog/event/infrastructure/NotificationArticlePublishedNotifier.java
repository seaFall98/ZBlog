package com.zblog.event.infrastructure;

import com.zblog.event.application.port.ArticlePublishedNotifier;
import com.zblog.notification.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class NotificationArticlePublishedNotifier implements ArticlePublishedNotifier {

  private final NotificationService notificationService;

  public NotificationArticlePublishedNotifier(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  public void notifyArticlePublished(long articleId, String title, String slug, long eventId) {
    notificationService.createArticlePublishedNotification(articleId, title, slug, eventId);
  }
}
