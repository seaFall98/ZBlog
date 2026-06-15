package com.zblog.scheduler;

import com.zblog.notification.application.port.NotificationRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationCleanupJobHandler implements ScheduledJobHandler {

  private final NotificationRepository notificationRepository;

  public NotificationCleanupJobHandler(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  public String name() {
    return "notification-cleanup";
  }

  public String description() {
    return "Delete read notifications older than the retention window.";
  }

  public Map<String, Object> defaultParameters() {
    return Map.of("retention_days", 90);
  }

  public String execute(Map<String, Object> parameters) {
    int retentionDays = retentionDays(parameters);
    LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
    int deleted = notificationRepository.deleteReadOlderThan(threshold);
    return "Deleted " + deleted + " read notifications older than " + retentionDays + " days.";
  }

  private int retentionDays(Map<String, Object> parameters) {
    Object value = parameters.get("retention_days");
    if (value instanceof Number number) {
      return Math.max(1, number.intValue());
    }
    if (value instanceof String text && !text.isBlank()) {
      return Math.max(1, Integer.parseInt(text));
    }
    return 90;
  }
}
