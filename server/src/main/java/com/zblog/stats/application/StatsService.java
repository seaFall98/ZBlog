package com.zblog.stats.application;

import com.zblog.common.api.PageResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

  private final JdbcTemplate jdbcTemplate;

  public StatsService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> siteStats() {
    long totalArticles = count("select count(*) from articles where status = 'PUBLISHED'");
    long totalComments = count("select count(*) from comments where is_deleted = false");
    long totalFriends =
        count(
            """
            select count(*)
            from friends f
            left join friend_types ft on ft.id = f.type_id
            where f.is_pending = false and (ft.is_visible = true or ft.id is null)
            """);
    long totalCategories =
        count(
            """
            select count(distinct category_id)
            from articles
            where status = 'PUBLISHED' and category_id is not null
            """);
    long totalTags =
        count(
            """
            select count(distinct at.tag_id)
            from article_tags at
            join articles a on a.id = at.article_id
            where a.status = 'PUBLISHED'
            """);
    long articleViews = count("select coalesce(sum(view_count), 0) from articles");
    long visitPageViews = count("select count(*) from visit_events where event_type = 'pageview'");
    long totalViews = articleViews + visitPageViews;
    long totalWords = count("select coalesce(sum(length(content_text)), 0) from articles where status = 'PUBLISHED'");
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate monthStart = today.withDayOfMonth(1);

    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("total_words", Long.toString(totalWords));
    stats.put("total_visitors", count("select count(distinct visitor_id) from visit_events"));
    stats.put("total_page_views", totalViews);
    stats.put("online_users", countSince(Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(5))));
    stats.put("total_articles", totalArticles);
    stats.put("total_comments", totalComments);
    stats.put("total_friends", totalFriends);
    stats.put("total_moments", count("select count(*) from moments"));
    stats.put("total_categories", totalCategories);
    stats.put("total_tags", totalTags);
    stats.put("today_visitors", countDistinctVisitors(today, today.plusDays(1)));
    stats.put("today_pageviews", countPageviews(today, today.plusDays(1)));
    stats.put("yesterday_visitors", countDistinctVisitors(yesterday, today));
    stats.put("yesterday_pageviews", countPageviews(yesterday, today));
    stats.put("month_pageviews", countPageviews(monthStart, today.plusDays(1)));
    return stats;
  }

  public Map<String, Object> archiveStats() {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            """
            select published_at
            from articles
            where status = 'PUBLISHED' and published_at is not null
            order by published_at desc
            """);
    Map<String, Long> grouped = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
      Object value = row.get("published_at");
      if (value instanceof Timestamp timestamp) {
        YearMonth month = YearMonth.from(timestamp.toLocalDateTime());
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
    dashboard.put("total_moments", count("select count(*) from moments"));
    dashboard.put("total_views", site.get("total_page_views"));
    dashboard.put("total_visitors", site.get("total_visitors"));
    dashboard.put("total_comments", site.get("total_comments"));
    dashboard.put("total_users", count("select count(*) from users where deleted_at is null"));
    dashboard.put("today_views", site.get("today_pageviews"));
    dashboard.put("today_visitors", site.get("today_visitors"));
    dashboard.put("today_comments", count("select count(*) from comments where created_at >= current_date"));
    dashboard.put("today_users", count("select count(*) from users where created_at >= current_date and deleted_at is null"));
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
    return jdbcTemplate.queryForList(
        """
        select c.name, count(a.id) as count
        from categories c
        left join articles a on a.category_id = c.id and a.status = 'PUBLISHED'
        group by c.id, c.name
        order by count desc, c.id
        """);
  }

  public List<Map<String, Object>> tagStats() {
    return jdbcTemplate.queryForList(
        """
        select t.name, count(a.id) as count
        from tags t
        left join article_tags at on at.tag_id = t.id
        left join articles a on a.id = at.article_id and a.status = 'PUBLISHED'
        group by t.id, t.name
        order by count desc, t.id
        """);
  }

  public List<Map<String, Object>> contribution(Integer year, Integer month) {
    int targetYear = year == null ? LocalDate.now().getYear() : year;
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            "select published_at from articles where status = 'PUBLISHED' and published_at is not null");
    Map<String, Long> grouped = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
      Object value = row.get("published_at");
      if (value instanceof Timestamp timestamp) {
        LocalDate date = timestamp.toLocalDateTime().toLocalDate();
        if (date.getYear() == targetYear && (month == null || date.getMonthValue() == month)) {
          grouped.merge(date.format(DateTimeFormatter.ISO_DATE), 1L, Long::sum);
        }
      }
    }
    return grouped.entrySet().stream()
        .map(entry -> Map.<String, Object>of("date", entry.getKey(), "count", entry.getValue()))
        .toList();
  }

  public PageResponse<Map<String, Object>> visits(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    long total = count("select count(*) from visit_events");
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            """
            select id, visitor_id, ip, url, user_agent, referrer, event_type, event_name, created_at
            from visit_events
            order by created_at desc, id desc
            limit ? offset ?
            """,
            pageSize,
            offset)
            .stream()
            .map(this::visitRow)
            .toList();
    return new PageResponse<>(rows, total, page, pageSize);
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
    mapped.put("browser", "unsupported");
    mapped.put("os", "unsupported");
    mapped.put("created_at", row.get("created_at"));
    return mapped;
  }

  private Map<String, Map<String, Long>> dailyVisitGroups(LocalDate start, LocalDate end) {
    return visitGroups(
        """
        select cast(created_at as date) as period,
          count(*) as pv,
          count(distinct visitor_id) as uv
        from visit_events
        where event_type = 'pageview' and created_at >= ? and created_at < ?
        group by cast(created_at as date)
        """,
        java.sql.Date.valueOf(start),
        java.sql.Date.valueOf(end.plusDays(1)));
  }

  private Map<String, Map<String, Long>> monthlyVisitGroups(LocalDate start, LocalDate end) {
    Map<String, MonthlyVisitBucket> buckets = new LinkedHashMap<>();
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            """
            select visitor_id, created_at
            from visit_events
            where event_type = 'pageview' and created_at >= ? and created_at < ?
            """,
            java.sql.Date.valueOf(start),
            java.sql.Date.valueOf(end.plusDays(1)));
    for (Map<String, Object> row : rows) {
      Object value = row.get("created_at");
      if (value instanceof Timestamp timestamp) {
        String period = YearMonth.from(timestamp.toLocalDateTime()).toString();
        buckets.computeIfAbsent(period, ignored -> new MonthlyVisitBucket()).add(row.get("visitor_id"));
      }
    }
    Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
    for (Map.Entry<String, MonthlyVisitBucket> entry : buckets.entrySet()) {
      grouped.put(entry.getKey(), entry.getValue().toMap());
    }
    return grouped;
  }

  private Map<String, Map<String, Long>> visitGroups(String sql, Object... args) {
    Map<String, Map<String, Long>> grouped = new LinkedHashMap<>();
    for (Map<String, Object> row : jdbcTemplate.queryForList(sql, args)) {
      grouped.put(
          row.get("period").toString(),
          Map.of("pv", ((Number) row.get("pv")).longValue(), "uv", ((Number) row.get("uv")).longValue()));
    }
    return grouped;
  }

  private long countSince(Timestamp since) {
    return count("select count(distinct visitor_id) from visit_events where created_at >= ?", since);
  }

  private long countDistinctVisitors(LocalDate start, LocalDate end) {
    return count(
        "select count(distinct visitor_id) from visit_events where created_at >= ? and created_at < ?",
        java.sql.Date.valueOf(start),
        java.sql.Date.valueOf(end));
  }

  private long countPageviews(LocalDate start, LocalDate end) {
    return count(
        "select count(*) from visit_events where event_type = 'pageview' and created_at >= ? and created_at < ?",
        java.sql.Date.valueOf(start),
        java.sql.Date.valueOf(end));
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

  private long count(String sql, Object... args) {
    Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
    return value == null ? 0 : value.longValue();
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
