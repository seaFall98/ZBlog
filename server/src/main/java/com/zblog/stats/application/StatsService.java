package com.zblog.stats.application;

import com.zblog.common.api.PageResponse;
import com.zblog.stats.application.port.StatsCache;
import com.zblog.stats.application.port.StatsRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

  // 统计服务输出面向前台和后台的 read-model，不代表单一领域聚合。
  private final StatsRepository statsRepository;
  private final StatsCache statsCache;

  public StatsService(StatsRepository statsRepository, StatsCache statsCache) {
    this.statsRepository = statsRepository;
    this.statsCache = statsCache;
  }

  public Map<String, Object> siteStats() {
    if (statsCache.siteStatsCacheSeconds() <= 0) {
      return computeSiteStats();
    }
    // 站点统计允许短 TTL 缓存，采集写入会主动失效。
    return statsCache
        .siteStats()
        .orElseGet(
            () -> {
              Map<String, Object> stats = computeSiteStats();
              statsCache.cacheSiteStats(stats, Duration.ofSeconds(statsCache.siteStatsCacheSeconds()));
              return stats;
            });
  }

  private Map<String, Object> computeSiteStats() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate monthStart = today.withDayOfMonth(1);

    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("total_words", Long.toString(statsRepository.countPublishedWords()));
    stats.put("total_visitors", statsRepository.countVisitors());
    stats.put("total_page_views", statsRepository.countPageviewEvents());
    stats.put("online_users", statsRepository.countOnlineVisitors(LocalDateTime.now().minusMinutes(5)));
    stats.put("total_articles", statsRepository.countPublishedArticles());
    stats.put("total_comments", statsRepository.countVisibleComments());
    stats.put("total_friends", statsRepository.countVisibleFriends());
    stats.put("total_moments", statsRepository.countMoments());
    stats.put("total_photos", statsRepository.countAlbumPhotos());
    stats.put("total_guestbook_messages", statsRepository.countGuestbookMessages());
    stats.put("total_categories", statsRepository.countPublishedCategories());
    stats.put("total_tags", statsRepository.countPublishedTags());
    stats.put("today_visitors", statsRepository.countVisitors(today, today.plusDays(1)));
    stats.put("today_pageviews", statsRepository.countPageviews(today, today.plusDays(1)));
    stats.put("yesterday_visitors", statsRepository.countVisitors(yesterday, today));
    stats.put("yesterday_pageviews", statsRepository.countPageviews(yesterday, today));
    stats.put("month_pageviews", statsRepository.countPageviews(monthStart, today.plusDays(1)));
    return stats;
  }

  public Map<String, Object> archiveStats() {
    Map<String, Long> grouped = new LinkedHashMap<>();
    for (Map<String, Object> row : statsRepository.publishedArticleDates()) {
      Instant value = instant(row.get("published_at"));
      if (value != null) {
        YearMonth month = YearMonth.from(value.atZone(java.time.ZoneId.systemDefault()));
        grouped.merge(month.toString(), 1L, Long::sum);
      }
    }
    List<Map<String, Object>> archives =
        grouped.entrySet().stream()
            .map(
                entry -> {
                  String[] parts = entry.getKey().split("-");
                  return Map.<String, Object>of(
                      "year", parts[0], "month", parts[1], "count", entry.getValue());
                })
            .toList();
    return Map.of("archives", archives);
  }

  public Map<String, Object> dashboard() {
    Map<String, Object> site = siteStats();
    Map<String, Object> dashboard = new LinkedHashMap<>();
    dashboard.put("total_articles", site.get("total_articles"));
    dashboard.put("total_friends", site.get("total_friends"));
    dashboard.put("total_moments", statsRepository.countMoments());
    dashboard.put("total_views", site.get("total_page_views"));
    dashboard.put("total_visitors", site.get("total_visitors"));
    dashboard.put("total_comments", site.get("total_comments"));
    dashboard.put("total_users", statsRepository.countActiveUsers());
    dashboard.put("today_views", site.get("today_pageviews"));
    dashboard.put("today_visitors", site.get("today_visitors"));
    dashboard.put("today_comments", statsRepository.countTodayComments());
    dashboard.put("today_users", statsRepository.countTodayUsers());
    dashboard.put("views_growth", growth(number(site.get("today_pageviews")), number(site.get("yesterday_pageviews"))));
    dashboard.put("visitors_growth", growth(number(site.get("today_visitors")), number(site.get("yesterday_visitors"))));
    dashboard.put("comments_growth", 0);
    dashboard.put("users_growth", 0);
    return dashboard;
  }

  public List<Map<String, Object>> trend(String startDate, String endDate, String type) {
    LocalDate start = parseDate(startDate, LocalDate.now().minusDays(6));
    LocalDate end = parseDate(endDate, LocalDate.now());
    List<Map<String, Object>> result = new ArrayList<>();
    if ("monthly".equalsIgnoreCase(type)) {
      Map<String, Map<String, Long>> grouped = monthlyVisitGroups(start, end);
      YearMonth cursor = YearMonth.from(start);
      YearMonth last = YearMonth.from(end);
      while (!cursor.isAfter(last)) {
        Map<String, Long> row = grouped.getOrDefault(cursor.toString(), Map.of("pv", 0L, "uv", 0L));
        result.add(Map.of("date", cursor.toString(), "pv_count", row.get("pv"), "uv_count", row.get("uv")));
        cursor = cursor.plusMonths(1);
      }
      return result;
    }
    Map<String, Map<String, Long>> grouped = dailyVisitGroups(start, end);
    LocalDate cursor = start;
    while (!cursor.isAfter(end)) {
      Map<String, Long> row = grouped.getOrDefault(cursor.toString(), Map.of("pv", 0L, "uv", 0L));
      result.add(Map.of("date", cursor.toString(), "pv_count", row.get("pv"), "uv_count", row.get("uv")));
      cursor = cursor.plusDays(1);
    }
    return result;
  }

  public List<Map<String, Object>> categoryStats() {
    return statsRepository.categoryStats();
  }

  public List<Map<String, Object>> tagStats() {
    return statsRepository.tagStats();
  }

  public List<Map<String, Object>> contribution(Integer year, Integer month) {
    int targetYear = year == null ? LocalDate.now().getYear() : year;
    Map<String, Long> grouped = new LinkedHashMap<>();
    for (Map<String, Object> row : statsRepository.publishedArticleDates()) {
      Instant value = instant(row.get("published_at"));
      if (value != null) {
        LocalDate date = value.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        if (date.getYear() == targetYear && (month == null || date.getMonthValue() == month)) {
          grouped.merge(date.format(DateTimeFormatter.ISO_DATE), 1L, Long::sum);
        }
      }
    }
    return grouped.entrySet().stream()
        .map(entry -> Map.<String, Object>of("date", entry.getKey(), "count", entry.getValue()))
        .toList();
  }

  public PageResponse<Map<String, Object>> visits(
      int page,
      int pageSize,
      String keyword,
      String visitorId,
      String ip,
      String excludeIps,
      String location,
      String browser,
      String os,
      String startTime,
      String endTime) {
    return statsRepository.visits(
        new VisitQuery(page, pageSize, keyword, visitorId, ip, excludeIps, location, browser, os, startTime, endTime));
  }

  private Map<String, Map<String, Long>> dailyVisitGroups(LocalDate start, LocalDate end) {
    Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
    for (Map<String, Object> row : statsRepository.dailyVisitGroups(start, end)) {
      grouped.put(
          row.get("period").toString(),
          Map.of("pv", ((Number) row.get("pv")).longValue(), "uv", ((Number) row.get("uv")).longValue()));
    }
    return grouped;
  }

  private Map<String, Map<String, Long>> monthlyVisitGroups(LocalDate start, LocalDate end) {
    Map<String, MonthlyVisitBucket> buckets = new LinkedHashMap<>();
    for (Map<String, Object> row : statsRepository.monthlyVisitRows(start, end)) {
      Instant value = instant(row.get("created_at"));
      if (value != null) {
        String period = YearMonth.from(value.atZone(java.time.ZoneId.systemDefault())).toString();
        buckets.computeIfAbsent(period, ignored -> new MonthlyVisitBucket()).add(row.get("visitor_id"));
      }
    }
    Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
    for (Map.Entry<String, MonthlyVisitBucket> entry : buckets.entrySet()) {
      grouped.put(entry.getKey(), entry.getValue().toMap());
    }
    return grouped;
  }

  private Instant instant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
    if (value instanceof String text && !text.isBlank()) {
      return Instant.parse(text);
    }
    return null;
  }

  private int growth(long today, long yesterday) {
    if (yesterday == 0) {
      return today > 0 ? 100 : 0;
    }
    return (int) Math.round(((today - yesterday) * 100.0) / yesterday);
  }

  private long number(Object value) {
    return value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
  }

  private LocalDate parseDate(String value, LocalDate fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return LocalDate.parse(value);
  }

  private static final class MonthlyVisitBucket {
    private long pv;
    private final Set<Object> visitors = new HashSet<>();

    void add(Object visitorId) {
      pv++;
      visitors.add(visitorId);
    }

    Map<String, Long> toMap() {
      return Map.of("pv", pv, "uv", (long) visitors.size());
    }
  }
}
