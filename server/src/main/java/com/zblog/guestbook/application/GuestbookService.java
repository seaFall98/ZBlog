package com.zblog.guestbook.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.guestbook.application.port.GuestbookRepository;
import com.zblog.guestbook.domain.GuestbookMessage;
import com.zblog.guestbook.domain.GuestbookStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestbookService {

  private static final int MAX_NICKNAME_LENGTH = 64;
  private static final int MAX_EMAIL_LENGTH = 255;
  private static final int MAX_CONTENT_LENGTH = 500;

  private final GuestbookRepository repository;

  public GuestbookService(GuestbookRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Map<String, Object> submit(Map<String, Object> request, HttpServletRequest servletRequest) {
    String nickname = normalizeText(request.get("nickname"), "访客", MAX_NICKNAME_LENGTH, "nickname");
    String email = normalizeOptionalText(request.get("email"), MAX_EMAIL_LENGTH, "email");
    String content = normalizeRequiredText(request.get("content"), MAX_CONTENT_LENGTH, "content");
    GuestbookMessage message =
        repository.create(nickname, email, content, clientIp(servletRequest), servletRequest.getHeader("User-Agent"));
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", message.id());
    view.put("status", message.status().value());
    view.put("message", "已提交，审核通过后将展示。");
    return view;
  }

  public PageResponse<Map<String, Object>> listPublic(int page, int pageSize) {
    PageResponse<GuestbookMessage> result = repository.listPublic(page, pageSize);
    return new PageResponse<>(result.list().stream().map(this::publicView).toList(), result.total(), result.page(), result.pageSize());
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page,
      int pageSize,
      String keyword,
      String status,
      Boolean pinned,
      String startTime,
      String endTime) {
    GuestbookStatus parsedStatus = status == null || status.isBlank() ? null : parseStatus(status);
    PageResponse<GuestbookMessage> result =
        repository.listAdmin(page, pageSize, keyword, parsedStatus, pinned, startTime, endTime);
    return new PageResponse<>(result.list().stream().map(this::adminView).toList(), result.total(), result.page(), result.pageSize());
  }

  @Transactional
  public Map<String, Object> updateStatus(long id, Map<String, Object> request) {
    GuestbookStatus status = parseStatus(stringValue(request.get("status")));
    String adminNote = normalizeOptionalText(request.get("admin_note"), 500, "admin_note");
    return adminView(repository.updateStatus(id, status, adminNote));
  }

  @Transactional
  public Map<String, Object> updatePinned(long id, Map<String, Object> request) {
    Object value = request.get("pinned");
    if (!(value instanceof Boolean pinned)) {
      throw new BusinessException(40001, "pinned is required", HttpStatus.BAD_REQUEST);
    }
    return adminView(repository.updatePinned(id, pinned));
  }

  @Transactional
  public void delete(long id) {
    repository.delete(id);
  }

  private Map<String, Object> publicView(GuestbookMessage message) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", message.id());
    view.put("nickname", message.nickname());
    view.put("content", message.content());
    view.put("pinned", message.pinned());
    view.put("created_at", message.createdAt());
    return view;
  }

  private Map<String, Object> adminView(GuestbookMessage message) {
    Map<String, Object> view = publicView(message);
    view.put("email", message.email());
    view.put("status", message.status().value());
    view.put("ip", message.ip());
    view.put("user_agent", message.userAgent());
    view.put("admin_note", message.adminNote());
    view.put("updated_at", message.updatedAt());
    return view;
  }

  private GuestbookStatus parseStatus(String value) {
    try {
      return GuestbookStatus.from(value);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(40001, exception.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

  private String normalizeText(Object value, String fallback, int maxLength, String field) {
    String normalized = normalizeOptionalText(value, maxLength, field);
    return normalized == null || normalized.isBlank() ? fallback : normalized;
  }

  private String normalizeRequiredText(Object value, int maxLength, String field) {
    String normalized = normalizeOptionalText(value, maxLength, field);
    if (normalized == null || normalized.isBlank()) {
      throw new BusinessException(40001, field + " is required", HttpStatus.BAD_REQUEST);
    }
    return normalized;
  }

  private String normalizeOptionalText(Object value, int maxLength, String field) {
    String text = stringValue(value);
    if (text == null || text.isBlank()) {
      return null;
    }
    String trimmed = text.trim();
    if (trimmed.length() > maxLength) {
      throw new BusinessException(40001, field + " is too long", HttpStatus.BAD_REQUEST);
    }
    return trimmed;
  }

  private String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
