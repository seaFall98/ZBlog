package com.zblog.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventOutboxService {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final EventMessageBroker broker;

  public EventOutboxService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, EventMessageBroker broker) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.broker = broker;
  }

  public void createArticlePublishedEvent(Map<String, Object> article) {
    long articleId = ((Number) article.get("id")).longValue();
    Map<String, Object> payload =
        Map.of(
            "article_id", articleId,
            "title", article.get("title"),
            "slug", article.get("slug"));
    jdbcTemplate.update(
        """
        insert into event_outbox (event_type, aggregate_type, aggregate_id, payload, status)
        values ('ARTICLE_PUBLISHED', 'article', ?, ?, 'pending')
        """,
        articleId,
        writeJson(payload));
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize, String status) {
    int offset = Math.max(0, page - 1) * pageSize;
    String where = status == null || status.isBlank() ? "" : " where status = ?";
    Object[] countArgs = status == null || status.isBlank() ? new Object[] {} : new Object[] {status};
    Long total = jdbcTemplate.queryForObject("select count(*) from event_outbox" + where, Long.class, countArgs);
    Object[] listArgs =
        status == null || status.isBlank()
            ? new Object[] {pageSize, offset}
            : new Object[] {status, pageSize, offset};
    List<Map<String, Object>> rows =
        jdbcTemplate.query(
            "select * from event_outbox"
                + where
                + " order by created_at desc, id desc limit ? offset ?",
            (rs, rowNum) -> mapRow(rs),
            listArgs);
    return new PageResponse<>(rows, total == null ? 0 : total, page, pageSize);
  }

  @Transactional
  public Map<String, Object> publishPending() {
    List<OutboxEvent> events =
        jdbcTemplate.query(
            """
            select *
            from event_outbox
            where status in ('pending', 'failed') and attempts < 3
            order by created_at asc, id asc
            limit 20
            """,
            (rs, rowNum) -> toEvent(rs));
    int published = 0;
    int failed = 0;
    for (OutboxEvent event : events) {
      jdbcTemplate.update(
          "update event_outbox set status = 'processing', attempts = attempts + 1, updated_at = current_timestamp where id = ?",
          event.id());
      try {
        broker.publish(event);
        jdbcTemplate.update(
            """
            update event_outbox
            set status = 'sent', sent_at = current_timestamp, error_message = null, updated_at = current_timestamp
            where id = ?
            """,
            event.id());
        published++;
      } catch (RuntimeException exception) {
        jdbcTemplate.update(
            """
            update event_outbox
            set status = 'failed', error_message = ?, updated_at = current_timestamp
            where id = ?
            """,
            exception.getMessage(),
            event.id());
        failed++;
      }
    }
    return Map.of("published", published, "failed", failed, "total", events.size());
  }

  private OutboxEvent toEvent(ResultSet rs) throws SQLException {
    return new OutboxEvent(
        rs.getLong("id"),
        rs.getString("event_type"),
        rs.getString("aggregate_type"),
        rs.getLong("aggregate_id"),
        rs.getString("payload"),
        rs.getInt("attempts"));
  }

  private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", rs.getLong("id"));
    row.put("event_type", rs.getString("event_type"));
    row.put("aggregate_type", rs.getString("aggregate_type"));
    row.put("aggregate_id", rs.getLong("aggregate_id"));
    row.put("payload", rs.getString("payload"));
    row.put("status", rs.getString("status"));
    row.put("attempts", rs.getInt("attempts"));
    row.put("error_message", rs.getString("error_message"));
    row.put("sent_at", timestamp(rs, "sent_at"));
    row.put("created_at", timestamp(rs, "created_at"));
    row.put("updated_at", timestamp(rs, "updated_at"));
    return row;
  }

  private Object timestamp(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant().toString();
  }

  private String writeJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid event payload", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
