package com.zblog.mail.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.mail.application.port.MailOutboxRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMailOutboxRepository implements MailOutboxRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcMailOutboxRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(String audience, String type, String recipient, String subject, String body) {
    org.springframework.jdbc.support.KeyHolder keyHolder =
        new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
                  insert into mail_outbox (audience, mail_type, recipient, subject, body, status)
                  values (?, ?, ?, ?, ?, 'pending')
                  """,
                  java.sql.Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, audience);
          statement.setString(2, type);
          statement.setString(3, recipient);
          statement.setString(4, subject);
          statement.setString(5, body);
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }

  public void markSent(long id) {
    jdbcTemplate.update(
        """
        update mail_outbox
        set status = 'sent', sent_at = current_timestamp, error_message = null, updated_at = current_timestamp
        where id = ?
        """,
        id);
  }

  public void markFailed(long id, String errorMessage) {
    jdbcTemplate.update(
        """
        update mail_outbox
        set status = 'failed', error_message = ?, next_attempt_at = current_timestamp, updated_at = current_timestamp
        where id = ?
        """,
        errorMessage,
        id);
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize, String status) {
    int offset = Math.max(0, page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where 1 = 1");
    if (status != null && !status.isBlank()) {
      where.append(" and status = ?");
      args.add(status);
    }
    Long total = jdbcTemplate.queryForObject("select count(*) from mail_outbox" + where, Long.class, args.toArray());
    args.add(pageSize);
    args.add(offset);
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            """
            select id, audience, mail_type, recipient, subject, status, error_message,
              attempts, last_attempt_at, next_attempt_at, created_at, sent_at, updated_at
            from mail_outbox
            """
                + where
                + " order by created_at desc, id desc limit ? offset ?",
            args.toArray());
    rows.forEach(this::formatTimestamps);
    return new PageResponse<>(rows, total == null ? 0 : total, page, pageSize);
  }

  public List<Map<String, Object>> pendingForDelivery(int limit) {
    return jdbcTemplate.queryForList(
        """
        select id, recipient, subject, body
        from mail_outbox
        where status in ('pending', 'failed')
          and (next_attempt_at is null or next_attempt_at <= current_timestamp)
        order by created_at asc, id asc
        limit ?
        """,
        limit);
  }

  public void markAttempting(long id) {
    jdbcTemplate.update(
        """
        update mail_outbox
        set attempts = attempts + 1, last_attempt_at = current_timestamp, updated_at = current_timestamp
        where id = ?
        """,
        id);
  }

  private void formatTimestamps(Map<String, Object> row) {
    formatTimestamp(row, "last_attempt_at");
    formatTimestamp(row, "next_attempt_at");
    formatTimestamp(row, "created_at");
    formatTimestamp(row, "sent_at");
    formatTimestamp(row, "updated_at");
  }

  private void formatTimestamp(Map<String, Object> row, String key) {
    Object value = row.get(key);
    if (value != null) {
      row.put(key, value.toString());
    }
  }
}
