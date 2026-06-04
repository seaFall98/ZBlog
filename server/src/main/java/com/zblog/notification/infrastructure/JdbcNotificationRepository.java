package com.zblog.notification.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.notification.application.port.NotificationRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcNotificationRepository implements NotificationRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public JdbcNotificationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public long countAll() {
    return count("select count(*) from notifications");
  }

  public long countUnread() {
    return count("select count(*) from notifications where is_read = false");
  }

  public List<Map<String, Object>> list(int pageSize, int offset) {
    return jdbcTemplate.query(
        """
        select * from notifications
        order by created_at desc, id desc
        limit ? offset ?
        """,
        (rs, rowNum) -> mapRow(rs),
        pageSize,
        offset);
  }

  public Map<String, Object> get(long id) {
    return jdbcTemplate
        .query("select * from notifications where id = ?", (rs, rowNum) -> mapRow(rs), id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Notification not found", HttpStatus.NOT_FOUND));
  }

  public long countArticlePublished(long articleId) {
    Number value =
        jdbcTemplate.queryForObject(
            "select count(*) from notifications where type = 'article_published' and target_id = ?",
            Number.class,
            articleId);
    return value == null ? 0 : value.longValue();
  }

  public Map<String, Object> latestArticlePublished(long articleId) {
    return jdbcTemplate
        .query(
            "select * from notifications where type = 'article_published' and target_id = ? order by id desc limit 1",
            (rs, rowNum) -> mapRow(rs),
            articleId)
        .getFirst();
  }

  public long create(
      String type,
      String title,
      String content,
      String link,
      Map<String, Object> data,
      Long targetId,
      String sender) {
    org.springframework.jdbc.support.KeyHolder keyHolder =
        new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
                  insert into notifications (type, title, content, link, data, target_id, sender)
                  values (?, ?, ?, ?, ?, ?, ?)
                  """,
                  java.sql.Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, type);
          statement.setString(2, title);
          statement.setString(3, content);
          statement.setString(4, link);
          statement.setString(5, writeJson(data));
          statement.setObject(6, targetId);
          statement.setString(7, sender);
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }

  public void markRead(long id) {
    jdbcTemplate.update(
        "update notifications set is_read = true, read_at = current_timestamp where id = ?", id);
  }

  public int markAllRead() {
    return jdbcTemplate.update(
        "update notifications set is_read = true, read_at = current_timestamp where is_read = false");
  }

  private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    String type = rs.getString("type");
    row.put("id", rs.getLong("id"));
    row.put("type", type);
    row.put("type_text", typeText(type));
    row.put("title", rs.getString("title"));
    row.put("content", rs.getString("content"));
    row.put("link", rs.getString("link"));
    row.put("data", readJson(rs.getString("data")));
    row.put("target_id", rs.getObject("target_id"));
    row.put("is_read", rs.getBoolean("is_read"));
    row.put("read_at", rs.getTimestamp("read_at"));
    row.put("created_at", rs.getTimestamp("created_at"));
    row.put("sender", rs.getString("sender"));
    return row;
  }

  private String typeText(String type) {
    return switch (type) {
      case "feedback_new" -> "反馈投诉";
      case "comment_new" -> "新评论";
      case "friend_apply" -> "友链申请";
      case "article_published" -> "文章发布";
      default -> "系统通知";
    };
  }

  private Map<String, Object> readJson(String value) {
    try {
      return objectMapper.readValue(value, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid notification data", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String writeJson(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid notification data", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private long count(String sql) {
    Number value = jdbcTemplate.queryForObject(sql, Number.class);
    return value == null ? 0 : value.longValue();
  }
}
