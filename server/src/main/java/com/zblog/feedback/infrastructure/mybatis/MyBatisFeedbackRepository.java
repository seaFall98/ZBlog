package com.zblog.feedback.infrastructure.mybatis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.feedback.application.port.FeedbackRepository;
import com.zblog.feedback.domain.FeedbackActorType;
import com.zblog.feedback.domain.FeedbackMessageType;
import com.zblog.feedback.domain.FeedbackStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisFeedbackRepository implements FeedbackRepository {

  private final FeedbackMapper feedbackMapper;
  private final ObjectMapper objectMapper;

  public MyBatisFeedbackRepository(FeedbackMapper feedbackMapper, ObjectMapper objectMapper) {
    this.feedbackMapper = feedbackMapper;
    this.objectMapper = objectMapper;
  }

  public long create(
      String ticketNo,
      String accessToken,
      Long userId,
      String reportUrl,
      String reportType,
      String formContentJson,
      String email,
      String userAgent,
      String ip) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("ticketNo", ticketNo);
    params.put("accessToken", accessToken);
    params.put("userId", userId);
    params.put("reportUrl", reportUrl);
    params.put("reportType", reportType);
    params.put("formContentJson", formContentJson);
    params.put("email", email);
    params.put("userAgent", userAgent);
    params.put("ip", ip);
    feedbackMapper.insertFeedback(params);
    return ((Number) params.get("id")).longValue();
  }

  public Map<String, Object> getByTicket(String ticketNo) {
    return feedbackMapper.rowsByTicket(ticketNo).stream()
        .findFirst()
        .map(this::mapPublicTicketRow)
        .orElseThrow(() -> new BusinessException(404, "Feedback not found", HttpStatus.NOT_FOUND));
  }

  public Map<String, Object> getByAccessToken(String accessToken) {
    return feedbackMapper.rowsByAccessToken(accessToken).stream()
        .findFirst()
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(404, "Feedback not found", HttpStatus.NOT_FOUND));
  }

  public PageResponse<Map<String, Object>> listByUserId(long userId, Map<String, String> params) {
    int page = number(params, "page", 1);
    int pageSize = number(params, "page_size", 10);
    int offset = Math.max(0, page - 1) * pageSize;
    String status = normalizeStatus(params.get("status"));
    return new PageResponse<>(
        feedbackMapper.listByUserId(userId, status, pageSize, offset).stream().map(this::mapRow).toList(),
        feedbackMapper.countByUserId(userId, status),
        page,
        pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(Map<String, String> params) {
    int page = number(params, "page", 1);
    int pageSize = number(params, "page_size", 10);
    int offset = Math.max(0, page - 1) * pageSize;
    String keyword = like(params.get("keyword"));
    String reportType = blankToNull(params.get("report_type"));
    String status = normalizeStatus(params.get("status"));
    LocalDateTime start = parseStart(params.get("start_time"));
    LocalDateTime end = parseEnd(params.get("end_time"));
    return new PageResponse<>(
        feedbackMapper.listAdmin(keyword, reportType, status, start, end, pageSize, offset).stream().map(this::mapRow).toList(),
        feedbackMapper.countAdmin(keyword, reportType, status, start, end),
        page,
        pageSize);
  }

  public Map<String, Object> get(long id) {
    return feedbackMapper.rowsById(id).stream()
        .findFirst()
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(404, "Feedback not found", HttpStatus.NOT_FOUND));
  }

  public void updateStatus(long id, String status) {
    feedbackMapper.updateStatus(id, status);
  }

  public void updateAdminReply(long id, String reply) {
    feedbackMapper.updateAdminReply(id, reply);
  }

  public void touchUserReply(long id) {
    feedbackMapper.touchUserReply(id);
  }

  public void insertMessage(
      long feedbackId,
      String actorType,
      Long actorUserId,
      String messageType,
      String content,
      String attachmentsJson,
      String fromStatus,
      String toStatus) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("feedbackId", feedbackId);
    params.put("actorType", actorType);
    params.put("actorUserId", actorUserId);
    params.put("messageType", messageType);
    params.put("content", content == null ? "" : content);
    params.put("attachmentsJson", attachmentsJson == null || attachmentsJson.isBlank() ? "[]" : attachmentsJson);
    params.put("fromStatus", fromStatus);
    params.put("toStatus", toStatus);
    feedbackMapper.insertMessage(params);
  }

  public List<Map<String, Object>> listMessages(long feedbackId) {
    return feedbackMapper.listMessages(feedbackId).stream().map(this::mapMessageRow).toList();
  }

  public void delete(long id) {
    feedbackMapper.delete(id);
  }

  public int deleteResolvedOrClosedOlderThan(LocalDateTime threshold) {
    return feedbackMapper.deleteResolvedOrClosedOlderThan(threshold);
  }

  public String ownerEmail() {
    List<String> values = feedbackMapper.ownerEmailValues();
    if (values.isEmpty() || values.getFirst().isBlank() || values.getFirst().endsWith("@example.com")) {
      return "zz1362410372@qq.com";
    }
    return values.getFirst();
  }

  private Map<String, Object> mapRow(Map<String, Object> source) {
    Map<String, Object> row = new LinkedHashMap<>(source);
    row.put("form_content", readJson(string(source.get("form_content"))));
    FeedbackStatus status = FeedbackStatus.from(string(source.get("status")));
    row.put("status", status.name());
    row.put("status_label", status.label());
    row.put("status_tone", status.tone());
    row.put("allowed_next_statuses", status.allowedNext().stream().map(FeedbackStatus::name).toList());
    row.put("messages", listMessages(((Number) source.get("id")).longValue()));
    return row;
  }

  private Map<String, Object> mapPublicTicketRow(Map<String, Object> source) {
    FeedbackStatus status = FeedbackStatus.from(string(source.get("status")));
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("ticket_no", source.get("ticket_no"));
    row.put("report_type", source.get("report_type"));
    row.put("report_url", source.get("report_url"));
    row.put("status", status.name());
    row.put("status_label", status.label());
    row.put("status_tone", status.tone());
    row.put("feedback_time", source.get("feedback_time"));
    row.put("updated_at", source.get("updated_at"));
    return row;
  }

  private Map<String, Object> mapMessageRow(Map<String, Object> source) {
    Map<String, Object> row = new LinkedHashMap<>(source);
    FeedbackActorType actorType = FeedbackActorType.from(string(source.get("actor_type")));
    FeedbackMessageType messageType = FeedbackMessageType.from(string(source.get("message_type")));
    row.put("actor_type", actorType.name());
    row.put("message_type", messageType.name());
    row.put("attachments", readJsonList(string(source.get("attachments_json"))));
    return row;
  }

  private Map<String, Object> readJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid feedback content", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private List<Object> readJsonList(String json) {
    try {
      return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid feedback attachment data", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private int number(Map<String, String> params, String key, int fallback) {
    String value = params.get(key);
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return Integer.parseInt(value);
  }

  private String like(String value) {
    String normalized = blankToNull(value);
    return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String normalizeStatus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return FeedbackStatus.from(value).name();
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

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }
}
