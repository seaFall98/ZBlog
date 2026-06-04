package com.zblog.feedback.infrastructure;

import com.zblog.feedback.application.port.FeedbackNotifier;
import com.zblog.notification.NotificationService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationFeedbackNotifier implements FeedbackNotifier {

  private final NotificationService notificationService;

  public NotificationFeedbackNotifier(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  public void notifyNewFeedback(Map<String, Object> feedback) {
    notificationService.createFeedbackNotification(feedback);
  }
}
