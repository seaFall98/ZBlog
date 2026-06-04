package com.zblog.event.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.event.application.port.EventOutboxRepository;
import com.zblog.event.domain.OutboxEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEventOutboxRepository implements EventOutboxRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcEventOutboxRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void createArticleEvent(String eventType, long aggregateId, String payload) {
    jdbcTemplate.update(
        """
        insert into event_outbox (event_type, aggregate_type, aggregate_id, payload, status)
        values (?, 'article', ?, ?, 'pending')
        """,
        eventType,
        aggregateId,
        payload);
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

  public List<OutboxEvent> pendingForPublish() {
    return jdbcTemplate.query(
        """
        select *
        from event_outbox
        where status in ('pending', 'failed') and attempts < 3
        order by created_at asc, id asc
        limit 20
        """,
        (rs, rowNum) -> toEvent(rs));
  }

  public void markProcessing(long eventId) {
    jdbcTemplate.update(
        "update event_outbox set status = 'processing', attempts = attempts + 1, updated_at = current_timestamp where id = ?",
        eventId);
  }

  public void markSent(long eventId) {
    jdbcTemplate.update(
        """
        update event_outbox
        set status = 'sent', sent_at = current_timestamp, error_message = null, updated_at = current_timestamp
        where id = ?
        """,
        eventId);
  }

  public void markFailed(long eventId, String errorMessage) {
    jdbcTemplate.update(
        """
        update event_outbox
        set status = 'failed', error_message = ?, updated_at = current_timestamp
        where id = ?
        """,
        errorMessage,
        eventId);
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
    return value == null ? null : value.toString();
  }
}
