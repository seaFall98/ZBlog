package com.zblog.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public NotificationService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> list(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    long total = count("select count(*) from notifications");
    long unread = count("select count(*) from notifications where is_read = false");
    List<Map<String, Object>> list =
        jdbcTemplate.query(
            """
            select * from notifications
            order by created_at desc, id desc
            limit ? offset ?
            """,
            (rs, rowNum) -> mapRow(rs),
            pageSize,
            offset);
    return Map.of("list", list, "total", total, "page", page, "page_size", pageSize, "unread_count", unread);
  }

  public Map<String, Object> createFeedbackNotification(Map<String, Object> feedback) {
    long targetId = ((Number) feedback.get("id")).longValue();
    String ticketNo = feedback.get("ticket_no").toString();
    String reportType = feedback.get("report_type").toString();
    String reportUrl = feedback.get("report_url").toString();
    Map<String, Object> data =
        Map.of(
            "ticket_no", ticketNo,
            "report_url", reportUrl,
            "report_type", reportType,
            "form_content", feedback.get("form_content"),
            "status", feedback.get("status"));
    long id =
        insert(
            "feedback_new",
            "新的反馈工单",
            "收到来自 " + feedback.get("email") + " 的反馈：" + ticketNo,
            "/feedback?ticket_no=" + ticketNo,
            data,
            targetId,
            "system");
    return get(id);
  }

  public Map<String, Object> markRead(long id) {
    jdbcTemplate.update(
        "update notifications set is_read = true, read_at = current_timestamp where id = ?", id);
    return get(id);
  }

  @Transactional
  public Map<String, Object> markAllRead() {
    int affected =
        jdbcTemplate.update(
            "update notifications set is_read = true, read_at = current_timestamp where is_read = false");
    return Map.of("affected", affected, "unread_count", 0);
  }

  private Map<String, Object> get(long id) {
    return jdbcTemplate
        .query("select * from notifications where id = ?", (rs, rowNum) -> mapRow(rs), id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Notification not found", HttpStatus.NOT_FOUND));
  }

  private long insert(
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
