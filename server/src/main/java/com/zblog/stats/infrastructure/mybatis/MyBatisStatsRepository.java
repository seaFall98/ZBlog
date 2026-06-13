package com.zblog.stats.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.util.AdminDateRange;
import com.zblog.stats.application.VisitQuery;
import com.zblog.stats.application.port.StatsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisStatsRepository implements StatsRepository {

  private final StatsMapper statsMapper;

  public MyBatisStatsRepository(StatsMapper statsMapper) {
    this.statsMapper = statsMapper;
  }

  public long countPublishedArticles() {
    return statsMapper.countPublishedArticles();
  }

  public long countVisibleComments() {
    return statsMapper.countVisibleComments();
  }

  public long countVisibleFriends() {
    return statsMapper.countVisibleFriends();
  }

  public long countPublishedCategories() {
    return statsMapper.countPublishedCategories();
  }

  public long countPublishedTags() {
    return statsMapper.countPublishedTags();
  }

  public long countPageviewEvents() {
    return statsMapper.countPageviewEvents();
  }

  public long countPublishedWords() {
    return statsMapper.countPublishedWords();
  }

  public long countVisitors() {
    return statsMapper.countVisitors();
  }

  public long countOnlineVisitors(LocalDateTime since) {
    return statsMapper.countOnlineVisitors(since);
  }

  public long countMoments() {
    return statsMapper.countMoments();
  }

  public long countAlbumPhotos() {
    return statsMapper.countAlbumPhotos();
  }

  public long countGuestbookMessages() {
    return statsMapper.countGuestbookMessages();
  }

  public long countVisitors(LocalDate start, LocalDate end) {
    return statsMapper.countVisitorsBetween(start, end);
  }

  public long countPageviews(LocalDate start, LocalDate end) {
    return statsMapper.countPageviewsBetween(start, end);
  }

  public List<Map<String, Object>> publishedArticleDates() {
    return statsMapper.publishedArticleDates();
  }

  public List<Map<String, Object>> archiveStats() {
    return statsMapper.archiveStats();
  }

  public long countActiveUsers() {
    return statsMapper.countActiveUsers();
  }

  public long countTodayComments() {
    return statsMapper.countTodayComments();
  }

  public long countTodayUsers() {
    return statsMapper.countTodayUsers();
  }

  public List<Map<String, Object>> dailyVisitGroups(LocalDate start, LocalDate end) {
    return statsMapper.dailyVisitGroups(start, end.plusDays(1));
  }

  public List<Map<String, Object>> monthlyVisitRows(LocalDate start, LocalDate end) {
    return statsMapper.monthlyVisitRows(start, end.plusDays(1));
  }

  public List<Map<String, Object>> categoryStats() {
    return statsMapper.categoryStats();
  }

  public List<Map<String, Object>> tagStats() {
    return statsMapper.tagStats();
  }

  public PageResponse<Map<String, Object>> visits(VisitQuery query) {
    AdminDateRange dateRange = AdminDateRange.parse(query.startTime(), query.endTime());
    List<Map<String, Object>> rows =
        statsMapper
            .visitRows(
                normalizedLike(query.keyword()),
                blankToNull(query.visitorId()),
                blankToNull(query.ip()),
                excludedIps(query.excludeIps()),
                dateRange.startInclusive(),
                dateRange.endExclusive())
            .stream()
            .map(this::visitRow)
            .filter(row -> blankToNull(query.location()) == null || row.get("location").toString().equalsIgnoreCase(query.location()))
            .filter(row -> blankToNull(query.browser()) == null || row.get("browser").toString().toLowerCase().contains(query.browser().toLowerCase()))
            .filter(row -> blankToNull(query.os()) == null || row.get("os").toString().toLowerCase().contains(query.os().toLowerCase()))
            .toList();
    int from = Math.min(Math.max(0, query.page() - 1) * query.pageSize(), rows.size());
    int to = Math.min(from + query.pageSize(), rows.size());
    return new PageResponse<>(rows.subList(from, to), rows.size(), query.page(), query.pageSize());
  }

  private Map<String, Object> visitRow(Map<String, Object> row) {
    Map<String, Object> mapped = new LinkedHashMap<>();
    mapped.put("id", row.get("id"));
    mapped.put("visitor_id", row.get("visitor_id"));
    mapped.put("ip", row.get("ip"));
    mapped.put("url", row.get("url"));
    mapped.put("page_url", row.get("url"));
    mapped.put("user_agent", row.get("user_agent"));
    mapped.put("referer", row.get("referrer"));
    mapped.put("event_type", row.get("event_type"));
    mapped.put("event_name", row.get("event_name"));
    mapped.put("location", "unsupported");
    mapped.put("browser", browser(row.get("user_agent")));
    mapped.put("os", os(row.get("user_agent")));
    mapped.put("created_at", row.get("created_at").toString());
    return mapped;
  }

  private String normalizedLike(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
  }

  private List<String> excludedIps(String value) {
    String normalized = blankToNull(value);
    if (normalized == null) {
      return List.of();
    }
    return Arrays.stream(normalized.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .toList();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String browser(Object userAgentValue) {
    String userAgent = userAgentValue == null ? "" : userAgentValue.toString();
    if (userAgent.contains("Edg/")) return "Edge";
    if (userAgent.contains("Chrome/")) return "Chrome";
    if (userAgent.contains("Firefox/")) return "Firefox";
    if (userAgent.contains("Safari/")) return "Safari";
    return "Unknown";
  }

  private String os(Object userAgentValue) {
    String userAgent = userAgentValue == null ? "" : userAgentValue.toString();
    if (userAgent.contains("Windows")) return "Windows";
    if (userAgent.contains("Mac OS X") || userAgent.contains("Macintosh")) return "macOS";
    if (userAgent.contains("Android")) return "Android";
    if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
    if (userAgent.contains("Linux")) return "Linux";
    return "Unknown";
  }
}
