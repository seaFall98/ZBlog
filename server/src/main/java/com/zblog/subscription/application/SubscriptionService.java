package com.zblog.subscription.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final JdbcTemplate jdbcTemplate;

  public SubscriptionService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public Map<String, Object> subscribe(Map<String, Object> request) {
    String email = text(request, "email").trim().toLowerCase();
    if (!EMAIL_PATTERN.matcher(email).matches()) {
      throw new BusinessException(40001, "Invalid email", HttpStatus.BAD_REQUEST);
    }
    List<Long> existing =
        jdbcTemplate.query(
            "select id from subscribers where email = ?",
            (rs, rowNum) -> rs.getLong("id"),
            email);
    if (existing.isEmpty()) {
      long id =
          insertAndReturnId(
              "insert into subscribers (email, unsubscribe_token, active) values (?, ?, true)",
              email,
              UUID.randomUUID().toString().replace("-", ""));
      return get(id);
    }
    long id = existing.getFirst();
    jdbcTemplate.update(
        """
        update subscribers
        set active = true, deleted_at = null, updated_at = current_timestamp
        where id = ?
        """,
        id);
    return get(id);
  }

  public Map<String, Object> unsubscribe(String token) {
    List<Long> ids =
        jdbcTemplate.query(
            "select id from subscribers where unsubscribe_token = ? and deleted_at is null",
            (rs, rowNum) -> rs.getLong("id"),
            token);
    if (ids.isEmpty()) {
      throw new BusinessException(404, "Subscriber not found", HttpStatus.NOT_FOUND);
    }
    long id = ids.getFirst();
    jdbcTemplate.update(
        "update subscribers set active = false, updated_at = current_timestamp where id = ?", id);
    return get(id);
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

  private Map<String, Object> get(long id) {
    return jdbcTemplate
        .query(
            """
            select id, email, active, created_at, updated_at
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
    return Map.of(
        "id",
        rs.getLong("id"),
        "email",
        rs.getString("email"),
        "active",
        rs.getBoolean("active"),
        "created_at",
        rs.getTimestamp("created_at"),
        "updated_at",
        rs.getTimestamp("updated_at"));
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

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString();
  }
}
