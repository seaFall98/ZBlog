package com.zblog.subscription.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.subscription.application.port.SubscriberRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcSubscriberRepository implements SubscriberRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcSubscriberRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Long> findIdsByEmail(String email) {
    return jdbcTemplate.query(
        "select id from subscribers where email = ?", (rs, rowNum) -> rs.getLong("id"), email);
  }

  public long create(String email, String unsubscribeToken) {
    return insertAndReturnId(
        "insert into subscribers (email, unsubscribe_token, active) values (?, ?, true)",
        email,
        unsubscribeToken);
  }

  public void reactivate(long id) {
    jdbcTemplate.update(
        """
        update subscribers
        set active = true, deleted_at = null, updated_at = current_timestamp
        where id = ?
        """,
        id);
  }

  public List<Long> findActiveIdsByToken(String token) {
    return jdbcTemplate.query(
        "select id from subscribers where unsubscribe_token = ? and deleted_at is null",
        (rs, rowNum) -> rs.getLong("id"),
        token);
  }

  public void deactivate(long id) {
    jdbcTemplate.update(
        "update subscribers set active = false, updated_at = current_timestamp where id = ?", id);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from subscribers where deleted_at is null", Long.class);
    List<Map<String, Object>> list =
        jdbcTemplate.query(
            """
            select id, email, active, created_at, updated_at
            from subscribers
            where deleted_at is null
            order by created_at desc, id desc
            limit ? offset ?
            """,
            (rs, rowNum) -> mapRow(rs),
            pageSize,
            offset);
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
  }

  public void delete(long id) {
    jdbcTemplate.update(
        "update subscribers set deleted_at = current_timestamp, active = false, updated_at = current_timestamp where id = ?",
        id);
  }

  public Map<String, Object> get(long id) {
    return jdbcTemplate
        .query(
            """
            select id, email, unsubscribe_token, active, created_at, updated_at
            from subscribers
            where id = ? and deleted_at is null
            """,
            (rs, rowNum) -> mapRow(rs),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new BusinessException(404, "Subscriber not found", HttpStatus.NOT_FOUND));
  }

  private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
    java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
    row.put("id", rs.getLong("id"));
    row.put("email", rs.getString("email"));
    try {
      row.put("unsubscribe_token", rs.getString("unsubscribe_token"));
    } catch (SQLException ignored) {
      row.put("unsubscribe_token", "");
    }
    row.put("active", rs.getBoolean("active"));
    row.put("created_at", rs.getTimestamp("created_at"));
    row.put("updated_at", rs.getTimestamp("updated_at"));
    return row;
  }

  @Transactional
  long insertAndReturnId(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
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
}
