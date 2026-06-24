package com.zblog.scheduler;

import com.zblog.stats.application.DailyStatsArchiveService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DailyVisitArchiveJobHandler implements ScheduledJobHandler {

  private final DailyStatsArchiveService archiveService;

  public DailyVisitArchiveJobHandler(DailyStatsArchiveService archiveService) {
    this.archiveService = archiveService;
  }

  public String name() {
    return "daily-visit-archive";
  }

  public String description() {
    return "Archive site and article visit statistics for a day.";
  }

  public Map<String, Object> defaultParameters() {
    return Map.of("stat_date", "");
  }

  public String execute(Map<String, Object> parameters) {
    LocalDate statDate = statDate(parameters);
    Map<String, Object> result = archiveService.archive(statDate);
    return "Archived " + result.get("stat_date") + " with " + result.get("article_rows") + " article rows.";
  }

  private LocalDate statDate(Map<String, Object> parameters) {
    Object value = parameters.get("stat_date");
    if (value instanceof String text && !text.isBlank()) {
      return LocalDate.parse(text);
    }
    return LocalDate.now().minusDays(1);
  }
}
