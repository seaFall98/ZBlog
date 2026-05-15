package com.zblog.rssfeed.application;

import com.zblog.common.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RssFeedAdminService {

  private final JdbcTemplate jdbcTemplate;

  public RssFeedAdminService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> listAdmin(Map<String, String> params) {
    int page = number(params, "page", 1);
    int pageSize = number(params, "page_size", 20);
    int offset = Math.max(0, page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    String where = buildWhere(params, args);
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from rss_feed_articles r left join friends f on f.id = r.friend_id " + where,
            Long.class,
            args.toArray());
    Long unreadCount =
        jdbcTemplate.queryForObject("select count(*) from rss_feed_articles where is_read = false", Long.class);
    List<Object> listArgs = new ArrayList<>(args);
    listArgs.add(pageSize);
    listArgs.add(offset);
    List<Map<String, Object>> list =
        jdbcTemplate.query(
            """
            select r.id, r.friend_id, coalesce(f.name, '') as friend_name, coalesce(f.url, '') as friend_url,
              r.title, r.link, r.description, r.published_at, r.is_read, r.created_at
            from rss_feed_articles r
            left join friends f on f.id = r.friend_id
            """
                + where
                + " order by coalesce(r.published_at, r.created_at) desc, r.id desc limit ? offset ?",
            (rs, rowNum) -> mapRow(rs),
            listArgs.toArray());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("list", list);
    result.put("total", total == null ? 0 : total);
    result.put("page", page);
    result.put("page_size", pageSize);
    result.put("unread_count", unreadCount == null ? 0 : unreadCount);
    return result;
  }

  public void markRead(long id) {
    jdbcTemplate.update("update rss_feed_articles set is_read = true where id = ?", id);
  }

  public Map<String, Object> markAllRead() {
    int affected = jdbcTemplate.update("update rss_feed_articles set is_read = true where is_read = false");
    return Map.of("affected", affected);
  }

  private String buildWhere(Map<String, String> params, List<Object> args) {
    StringBuilder where = new StringBuilder("where 1 = 1");
    String keyword = params.get("keyword");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" and (lower(r.title) like ? or lower(r.description) like ? or lower(f.name) like ?)");
      String like = "%" + keyword.toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    String friendId = params.get("friend_id");
    if (friendId != null && !friendId.isBlank()) {
      where.append(" and r.friend_id = ?");
      args.add(Long.parseLong(friendId));
    }
    String read = params.get("is_read");
    if (read != null && !read.isBlank()) {
      where.append(" and r.is_read = ?");
      args.add(Boolean.parseBoolean(read));
    }
    addTimeRange(where, args, params, "r.published_at");
    return where.toString();
  }

  private void addTimeRange(
      StringBuilder where, List<Object> args, Map<String, String> params, String column) {
    LocalDateTime start = parseStart(params.get("start_time"));
    LocalDateTime end = parseEnd(params.get("end_time"));
    if (start != null) {
      where.append(" and ").append(column).append(" >= ?");
      args.add(Timestamp.valueOf(start));
    }
    if (end != null) {
      where.append(" and ").append(column).append(" < ?");
      args.add(Timestamp.valueOf(end));
    }
  }

  private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", rs.getLong("id"));
    row.put("friend_id", rs.getLong("friend_id"));
    row.put("friend_name", rs.getString("friend_name"));
    row.put("friend_url", rs.getString("friend_url"));
    row.put("title", rs.getString("title"));
    row.put("link", rs.getString("link"));
    row.put("description", rs.getString("description"));
    row.put("published_at", rs.getTimestamp("published_at"));
    row.put("is_read", rs.getBoolean("is_read"));
    row.put("created_at", rs.getTimestamp("created_at"));
    return row;
  }

  private int number(Map<String, String> params, String key, int fallback) {
    String value = params.get(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return Integer.parseInt(value);
  }

  private LocalDateTime parseStart(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseDate(value).atStartOfDay();
  }

  private LocalDateTime parseEnd(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return parseDate(value).plusDays(1).atStartOfDay();
  }

  private LocalDate parseDate(String value) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(40001, "Invalid date", HttpStatus.BAD_REQUEST);
    }
  }
}
