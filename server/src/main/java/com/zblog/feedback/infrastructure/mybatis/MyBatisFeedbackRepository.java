package com.zblog.feedback.infrastructure.mybatis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.feedback.application.port.FeedbackRepository;
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
      String reportUrl,
      String reportType,
      String formContentJson,
      String email,
      String userAgent,
      String ip) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("ticketNo", ticketNo);
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
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(404, "Feedback not found", HttpStatus.NOT_FOUND));
  }

  public PageResponse<Map<String, Object>> listAdmin(Map<String, String> params) {
    int page = number(params, "page", 1);
    int pageSize = number(params, "page_size", 10);
    int offset = Math.max(0, page - 1) * pageSize;
    String keyword = like(params.get("keyword"));
    String reportType = blankToNull(params.get("report_type"));
    String status = blankToNull(params.get("status"));
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

  public void update(long id, String status, String reply) {
    feedbackMapper.update(id, status, reply);
  }

  public void delete(long id) {
    feedbackMapper.delete(id);
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
    return row;
  }

  private Map<String, Object> readJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid feedback content", HttpStatus.INTERNAL_SERVER_ERROR);
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
