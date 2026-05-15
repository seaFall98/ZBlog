package com.zblog.stats.application;

import com.zblog.common.api.PageResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    long totalViews = count("select coalesce(sum(view_count), 0) from articles");
    long totalWords = count("select coalesce(sum(length(content_text)), 0) from articles where status = 'PUBLISHED'");

    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("total_words", Long.toString(totalWords));
    stats.put("total_visitors", 0);
    stats.put("total_page_views", totalViews);
    stats.put("online_users", 0);
    stats.put("total_articles", totalArticles);
    stats.put("total_comments", totalComments);
    stats.put("total_friends", totalFriends);
    stats.put("total_moments", count("select count(*) from moments"));
    stats.put("total_categories", totalCategories);
    stats.put("total_tags", totalTags);
    stats.put("today_visitors", 0);
    stats.put("today_pageviews", 0);
    stats.put("yesterday_visitors", 0);
    stats.put("yesterday_pageviews", 0);
    stats.put("month_pageviews", 0);
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
    dashboard.put("total_visitors", 0);
    dashboard.put("total_comments", site.get("total_comments"));
    dashboard.put("total_users", 1);
    dashboard.put("today_views", 0);
    dashboard.put("today_visitors", 0);
    dashboard.put("today_comments", count("select count(*) from comments where created_at >= current_date"));
    dashboard.put("today_users", 0);
    dashboard.put("views_growth", 0);
    dashboard.put("visitors_growth", 0);
    dashboard.put("comments_growth", 0);
    dashboard.put("users_growth", 0);
    return dashboard;
  }

  public List<Map<String, Object>> trend(String startDate, String endDate, String type) {
    LocalDate start = parseDate(startDate, LocalDate.now().minusDays(6));
    LocalDate end = parseDate(endDate, LocalDate.now());
    List<Map<String, Object>> result = new ArrayList<>();
    if ("monthly".equalsIgnoreCase(type)) {
      YearMonth cursor = YearMonth.from(start);
      YearMonth last = YearMonth.from(end);
      while (!cursor.isAfter(last)) {
        result.add(Map.of("date", cursor.toString(), "pv_count", 0, "uv_count", 0));
        cursor = cursor.plusMonths(1);
      }
      return result;
    }
    LocalDate cursor = start;
    while (!cursor.isAfter(end)) {
      result.add(Map.of("date", cursor.toString(), "pv_count", 0, "uv_count", 0));
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
    return new PageResponse<>(List.of(), 0, page, pageSize);
  }

  private long count(String sql) {
    Number value = jdbcTemplate.queryForObject(sql, Number.class);
    return value == null ? 0 : value.longValue();
  }

  private LocalDate parseDate(String value, LocalDate fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return LocalDate.parse(value);
  }
}
