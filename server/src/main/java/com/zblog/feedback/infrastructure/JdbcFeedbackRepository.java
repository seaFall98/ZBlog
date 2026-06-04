package com.zblog.feedback.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.feedback.application.port.FeedbackRepository;
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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcFeedbackRepository implements FeedbackRepository {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public JdbcFeedbackRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public long create(
      String ticketNo,
      String reportUrl,
      String reportType,
      String formContentJson,
      String email,
      String userAgent,
      String ip) {
    return insertAndReturnId(
        """
        insert into feedbacks (
          ticket_no, report_url, report_type, form_content, email, user_agent, ip
        ) values (?, ?, ?, ?, ?, ?, ?)
        """,
        ticketNo,
        reportUrl,
        reportType,
        formContentJson,
        email,
        userAgent,
        ip);
  }

  public Map<String, Object> getByTicket(String ticketNo) {
    return jdbcTemplate
        .query(
            "select * from feedbacks where ticket_no = ? and deleted_at is null",
            (rs, rowNum) -> mapRow(rs),
            ticketNo)
        .stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Feedback not found", HttpStatus.NOT_FOUND));
  }

  public PageResponse<Map<String, Object>> listAdmin(Map<String, String> params) {
    int page = number(params, "page", 1);
    int pageSize = number(params, "page_size", 10);
    int offset = Math.max(0, page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    String where = buildWhere(params, args);
    Long total =
        jdbcTemplate.queryForObject("select count(*) from feedbacks " + where, Long.class, args.toArray());
    List<Object> listArgs = new ArrayList<>(args);
    listArgs.add(pageSize);
    listArgs.add(offset);
    List<Map<String, Object>> list =
        jdbcTemplate.query(
            "select * from feedbacks "
                + where
                + " order by feedback_time desc, id desc limit ? offset ?",
            (rs, rowNum) -> mapRow(rs),
            listArgs.toArray());
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
  }

  public Map<String, Object> get(long id) {
    return jdbcTemplate
        .query(
            "select * from feedbacks where id = ? and deleted_at is null",
            (rs, rowNum) -> mapRow(rs),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Feedback not found", HttpStatus.NOT_FOUND));
  }

  public void update(long id, String status, String reply) {
    jdbcTemplate.update(
        """
        update feedbacks
        set status = ?, admin_reply = ?, reply_time = case when ? <> '' then current_timestamp else reply_time end
        where id = ? and deleted_at is null
        """,
        status,
        reply,
        reply,
        id);
  }

  public void delete(long id) {
    jdbcTemplate.update("update feedbacks set deleted_at = current_timestamp where id = ?", id);
  }

  public String ownerEmail() {
    List<String> values =
        jdbcTemplate.query(
            "select value_text from settings where key_name = 'basic.author_email'",
            (rs, rowNum) -> rs.getString("value_text"));
    if (values.isEmpty() || values.getFirst().isBlank() || values.getFirst().endsWith("@example.com")) {
      return "zz1362410372@qq.com";
    }
    return values.getFirst();
  }

  private String buildWhere(Map<String, String> params, List<Object> args) {
    StringBuilder where = new StringBuilder("where deleted_at is null");
    String keyword = params.get("keyword");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" and (lower(ticket_no) like ? or lower(report_url) like ? or lower(email) like ?)");
      String like = "%" + keyword.toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    addEquals(where, args, params, "report_type");
    addEquals(where, args, params, "status");
    addTimeRange(where, args, params, "feedback_time");
    return where.toString();
  }

  private void addEquals(
      StringBuilder where, List<Object> args, Map<String, String> params, String column) {
    String value = params.get(column);
    if (value != null && !value.isBlank()) {
      where.append(" and ").append(column).append(" = ?");
      args.add(value);
    }
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
    row.put("ticket_no", rs.getString("ticket_no"));
    row.put("report_url", rs.getString("report_url"));
    row.put("report_type", rs.getString("report_type"));
    row.put("form_content", readJson(rs.getString("form_content")));
    row.put("email", rs.getString("email"));
    row.put("status", rs.getString("status"));
    row.put("admin_reply", rs.getString("admin_reply"));
    row.put("reply_time", rs.getTimestamp("reply_time"));
    row.put("user_agent", rs.getString("user_agent"));
    row.put("ip", rs.getString("ip"));
    row.put("feedback_time", rs.getTimestamp("feedback_time"));
    return row;
  }

  private Map<String, Object> readJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid feedback content", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Transactional
  long insertAndReturnId(String sql, Object... args) {
    org.springframework.jdbc.support.KeyHolder keyHolder =
        new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
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
