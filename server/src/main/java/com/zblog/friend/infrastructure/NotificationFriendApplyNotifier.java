package com.zblog.friend.infrastructure;

import com.zblog.notification.NotificationService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationFriendApplyNotifier {

  private final NotificationService notificationService;

  public NotificationFriendApplyNotifier(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  public void notifyNewFriendApply(Map<String, Object> friend) {
    notificationService.createFriendApplyNotification(friend);
  }
}
