package com.zblog.guestbook.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.guestbook.application.port.GuestbookRepository;
import com.zblog.guestbook.domain.GuestbookMessage;
import com.zblog.guestbook.domain.GuestbookStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisGuestbookRepository implements GuestbookRepository {

  private final GuestbookMapper guestbookMapper;

  public MyBatisGuestbookRepository(GuestbookMapper guestbookMapper) {
    this.guestbookMapper = guestbookMapper;
  }

  public GuestbookMessage create(String nickname, String email, String content, String ip, String userAgent) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("nickname", nickname);
    params.put("email", email);
    params.put("content", content);
    params.put("ip", ip);
    params.put("userAgent", userAgent);
    guestbookMapper.insertMessage(params);
    Object key = params.get("id");
    if (!(key instanceof Number number)) {
      throw new BusinessException(500, "Failed to create guestbook message", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return findById(number.longValue());
  }

  public PageResponse<GuestbookMessage> listPublic(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    return new PageResponse<>(
        guestbookMapper.listPublic(normalizedPageSize, offset).stream().map(this::mapRow).toList(),
        guestbookMapper.countPublic(),
        normalizedPage,
        normalizedPageSize);
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
    String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
    String statusValue = status == null ? null : status.value();
    LocalDateTime start = parseBoundary(startTime, false);
    LocalDateTime end = parseBoundary(endTime, true);
    return new PageResponse<>(
        guestbookMapper.listAdmin(like, statusValue, pinned, start, end, normalizedPageSize, offset).stream()
            .map(this::mapRow)
            .toList(),
        guestbookMapper.countAdmin(like, statusValue, pinned, start, end),
        normalizedPage,
        normalizedPageSize);
  }

  public GuestbookMessage updateStatus(long id, GuestbookStatus status, String adminNote) {
    findById(id);
    guestbookMapper.updateStatus(id, status.value(), adminNote);
    return findById(id);
  }

  public GuestbookMessage updatePinned(long id, boolean pinned) {
    findById(id);
    guestbookMapper.updatePinned(id, pinned);
    return findById(id);
  }

  public void delete(long id) {
    findById(id);
    guestbookMapper.delete(id);
  }

  public GuestbookMessage findById(long id) {
    return guestbookMapper.rowsById(id).stream()
        .findFirst()
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(404, "Guestbook message not found", HttpStatus.NOT_FOUND));
  }

  private GuestbookMessage mapRow(Map<String, Object> row) {
    return new GuestbookMessage(
        number(row.get("id")),
        string(row.get("nickname")),
        string(row.get("email")),
        string(row.get("content")),
        GuestbookStatus.from(string(row.get("status"))),
        bool(row.get("pinned")),
        string(row.get("ip")),
        string(row.get("user_agent")),
        string(row.get("admin_note")),
        bool(row.get("deleted")),
        localDateTime(row.get("created_at")),
        localDateTime(row.get("updated_at")));
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

  private long number(Object value) {
    return ((Number) value).longValue();
  }

  private boolean bool(Object value) {
    return Boolean.TRUE.equals(value);
  }

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }

  private LocalDateTime localDateTime(Object value) {
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toLocalDateTime();
    }
    return (LocalDateTime) value;
  }
}
