package com.zblog.feedback.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.feedback.application.port.FeedbackMailer;
import com.zblog.feedback.application.port.FeedbackNotifier;
import com.zblog.feedback.application.port.FeedbackRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

  private static final List<String> REPORT_TYPES =
      List.of("copyright", "inappropriate", "summary", "suggestion");
  private static final List<String> STATUSES = List.of("pending", "resolved", "closed");

  private final FeedbackRepository feedbackRepository;
  private final FeedbackNotifier feedbackNotifier;
  private final FeedbackMailer feedbackMailer;
  private final ObjectMapper objectMapper;

  public FeedbackService(
      FeedbackRepository feedbackRepository,
      FeedbackNotifier feedbackNotifier,
      FeedbackMailer feedbackMailer,
      ObjectMapper objectMapper) {
    this.feedbackRepository = feedbackRepository;
    this.feedbackNotifier = feedbackNotifier;
    this.feedbackMailer = feedbackMailer;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Map<String, Object> submit(Map<String, Object> request, HttpServletRequest servletRequest) {
    String reportType = text(request, "reportType", text(request, "report_type", "suggestion"));
    if (!REPORT_TYPES.contains(reportType)) {
      throw new BusinessException(40001, "Invalid report type", HttpStatus.BAD_REQUEST);
    }
    String ticketNo = "FB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    Map<String, Object> formContent = new LinkedHashMap<>();
    formContent.put("description", text(request, "description", ""));
    if (request.containsKey("reason")) {
      formContent.put("reason", text(request, "reason", ""));
    }
    if (request.containsKey("attachmentFiles")) {
      formContent.put("attachmentFiles", request.get("attachmentFiles"));
    }
    long id =
        feedbackRepository.create(
            ticketNo,
            text(request, "reportUrl", text(request, "report_url", "")),
            reportType,
            writeJson(formContent),
            text(request, "email", ""),
            servletRequest.getHeader("User-Agent") == null ? "" : servletRequest.getHeader("User-Agent"),
            clientIp(servletRequest));
    Map<String, Object> feedback = get(id);
    feedbackNotifier.notifyNewFeedback(feedback);
    feedbackMailer.sendNewFeedback(feedbackRepository.ownerEmail(), ticketNo, formContent);
    return feedback;
  }

  public Map<String, Object> getByTicket(String ticketNo) {
    return feedbackRepository.getByTicket(ticketNo);
  }

  public PageResponse<Map<String, Object>> listAdmin(Map<String, String> params) {
    return feedbackRepository.listAdmin(params);
  }

  public Map<String, Object> get(long id) {
    return feedbackRepository.get(id);
  }

  public Map<String, Object> update(long id, Map<String, Object> request) {
    String status = text(request, "status", "pending");
    if (!STATUSES.contains(status)) {
      throw new BusinessException(40001, "Invalid feedback status", HttpStatus.BAD_REQUEST);
    }
    String reply = text(request, "admin_reply", "");
    feedbackRepository.update(id, status, reply);
    Map<String, Object> feedback = get(id);
    String recipient = text(feedback, "email", "");
    if (!recipient.isBlank() && !reply.isBlank()) {
      feedbackMailer.sendReply(feedback, recipient, reply);
    }
    return feedback;
  }

  public void delete(long id) {
    feedbackRepository.delete(id);
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
  }

  private String text(Map<String, Object> request, String key, String fallback) {
    Object value = request.get(key);
    return value == null ? fallback : value.toString();
  }

  private String writeJson(Map<String, Object> content) {
    try {
      return objectMapper.writeValueAsString(content);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid feedback content", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
