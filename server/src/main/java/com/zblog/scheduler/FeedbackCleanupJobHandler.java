package com.zblog.scheduler;

import com.zblog.feedback.application.port.FeedbackRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FeedbackCleanupJobHandler implements ScheduledJobHandler {

  private final FeedbackRepository feedbackRepository;

  public FeedbackCleanupJobHandler(FeedbackRepository feedbackRepository) {
    this.feedbackRepository = feedbackRepository;
  }

  public String name() {
    return "feedback-cleanup";
  }

  public String description() {
    return "Delete resolved or closed feedback tickets older than the retention window.";
  }

  public Map<String, Object> defaultParameters() {
    return Map.of("retention_days", 90);
  }

  public String execute(Map<String, Object> parameters) {
    int retentionDays = retentionDays(parameters);
    LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
    int deleted = feedbackRepository.deleteResolvedOrClosedOlderThan(threshold);
    return "Deleted " + deleted + " resolved/closed feedback tickets older than " + retentionDays + " days.";
  }

  private int retentionDays(Map<String, Object> parameters) {
    Object snake = parameters.get("retention_days");
    Object camel = parameters.get("retentionDays");
    Object value = snake == null ? camel : snake;
    if (value instanceof Number number) {
      return Math.max(1, number.intValue());
    }
    if (value instanceof String text && !text.isBlank()) {
      return Math.max(1, Integer.parseInt(text));
    }
    return 90;
  }
}
