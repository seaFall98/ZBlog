package com.zblog.guestbook.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.guestbook.application.port.GuestbookRepository;
import com.zblog.guestbook.domain.GuestbookMessage;
import com.zblog.guestbook.domain.GuestbookStatus;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcGuestbookRepository implements GuestbookRepository {

  private final JdbcTemplate jdbcTemplate;

  private final RowMapper<GuestbookMessage> mapper =
      (rs, rowNum) ->
          new GuestbookMessage(
              rs.getLong("id"),
              rs.getString("nickname"),
              rs.getString("email"),
              rs.getString("content"),
              GuestbookStatus.from(rs.getString("status")),
              rs.getBoolean("pinned"),
              rs.getString("ip"),
              rs.getString("user_agent"),
              rs.getString("admin_note"),
              rs.getBoolean("deleted"),
              rs.getTimestamp("created_at").toLocalDateTime(),
              rs.getTimestamp("updated_at").toLocalDateTime());

  public JdbcGuestbookRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public GuestbookMessage create(String nickname, String email, String content, String ip, String userAgent) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  insert into guestbook_messages (nickname, email, content, status, pinned, ip, user_agent)
                  values (?, ?, ?, 'approved', false, ?, ?)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, nickname);
          statement.setString(2, email);
          statement.setString(3, content);
          statement.setString(4, ip);
          statement.setString(5, userAgent);
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    Object key = keys == null ? null : keys.get("id");
    if (!(key instanceof Number number)) {
      throw new BusinessException(500, "Failed to create guestbook message", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return findById(number.longValue());
  }

  public PageResponse<GuestbookMessage> listPublic(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    Long total =
        jdbcTemplate.queryForObject(
            "select count(*) from guestbook_messages where deleted = false and status = 'approved'", Long.class);
    List<GuestbookMessage> list =
        jdbcTemplate.query(
            """
            select * from guestbook_messages
            where deleted = false and status = 'approved'
            order by pinned desc, created_at desc, id desc
            limit ? offset ?
            """,
            mapper,
            normalizedPageSize,
            offset);
    return new PageResponse<>(list, total == null ? 0 : total, normalizedPage, normalizedPageSize);
  }

  public PageResponse<GuestbookMessage> listAdmin(
      int page,
      int pageSize,
      String keyword,
      GuestbookStatus status,
      Boolean pinned,
      String startTime,
      String endTime) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where deleted = false ");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" and (lower(nickname) like ? or lower(content) like ? or lower(coalesce(email, '')) like ?) ");
      String like = "%" + keyword.trim().toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (status != null) {
      where.append(" and status = ? ");
      args.add(status.value());
    }
    if (pinned != null) {
      where.append(" and pinned = ? ");
      args.add(pinned);
    }
    LocalDateTime start = parseBoundary(startTime, false);
    LocalDateTime end = parseBoundary(endTime, true);
    if (start != null) {
      where.append(" and created_at >= ? ");
      args.add(Timestamp.valueOf(start));
    }
    if (end != null) {
      where.append(" and created_at < ? ");
      args.add(Timestamp.valueOf(end));
    }

    Long total = jdbcTemplate.queryForObject("select count(*) from guestbook_messages" + where, Long.class, args.toArray());
    List<Object> listArgs = new ArrayList<>(args);
    listArgs.add(normalizedPageSize);
    listArgs.add(offset);
    List<GuestbookMessage> list =
        jdbcTemplate.query(
            "select * from guestbook_messages"
                + where
                + " order by pinned desc, created_at desc, id desc limit ? offset ?",
            mapper,
            listArgs.toArray());
    return new PageResponse<>(list, total == null ? 0 : total, normalizedPage, normalizedPageSize);
  }

  public GuestbookMessage updateStatus(long id, GuestbookStatus status, String adminNote) {
    findById(id);
    jdbcTemplate.update(
        """
        update guestbook_messages
        set status = ?, admin_note = ?, updated_at = current_timestamp
        where id = ? and deleted = false
        """,
        status.value(),
        adminNote,
        id);
    return findById(id);
  }

  public GuestbookMessage updatePinned(long id, boolean pinned) {
    findById(id);
    jdbcTemplate.update(
        "update guestbook_messages set pinned = ?, updated_at = current_timestamp where id = ? and deleted = false",
        pinned,
        id);
    return findById(id);
  }

  public void delete(long id) {
    findById(id);
    jdbcTemplate.update(
        "update guestbook_messages set deleted = true, updated_at = current_timestamp where id = ?", id);
  }

  public GuestbookMessage findById(long id) {
    List<GuestbookMessage> rows =
        jdbcTemplate.query("select * from guestbook_messages where id = ? and deleted = false", mapper, id);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Guestbook message not found", HttpStatus.NOT_FOUND);
    }
    return rows.getFirst();
  }

  private LocalDateTime parseBoundary(String value, boolean end) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    try {
      if (trimmed.length() == 10) {
        LocalDate date = LocalDate.parse(trimmed);
        return end ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
      }
      return LocalDateTime.parse(trimmed);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(40001, "invalid date range", HttpStatus.BAD_REQUEST);
    }
  }
}
