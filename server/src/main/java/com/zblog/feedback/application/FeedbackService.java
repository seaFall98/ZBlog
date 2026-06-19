package com.zblog.feedback.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.feedback.application.port.FeedbackMailer;
import com.zblog.feedback.application.port.FeedbackNotifier;
import com.zblog.feedback.application.port.FeedbackRepository;
import com.zblog.feedback.domain.FeedbackActorType;
import com.zblog.feedback.domain.FeedbackMessageType;
import com.zblog.feedback.domain.FeedbackStatus;
import com.zblog.feedback.domain.FeedbackType;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.identity.domain.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

  private final FeedbackRepository feedbackRepository;
  private final FeedbackNotifier feedbackNotifier;
  private final FeedbackMailer feedbackMailer;
  private final UserRepository userRepository;
  private final FeedbackStatusTransitionPolicy transitionPolicy;
  private final ObjectMapper objectMapper;

  public FeedbackService(
      FeedbackRepository feedbackRepository,
      FeedbackNotifier feedbackNotifier,
      FeedbackMailer feedbackMailer,
      UserRepository userRepository,
      FeedbackStatusTransitionPolicy transitionPolicy,
      ObjectMapper objectMapper) {
    this.feedbackRepository = feedbackRepository;
    this.feedbackNotifier = feedbackNotifier;
    this.feedbackMailer = feedbackMailer;
    this.userRepository = userRepository;
    this.transitionPolicy = transitionPolicy;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Map<String, Object> submit(
      Map<String, Object> request, HttpServletRequest servletRequest, Principal principal) {
    FeedbackType reportType = feedbackType(text(request, "reportType", text(request, "report_type", "suggestion")));
    String description = requiredText(request, "description");
    String ticketNo = "FB" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    String accessToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    UserAccount user = principal == null ? null : userRepository.findByEmail(principal.getName());
    Map<String, Object> formContent = new LinkedHashMap<>();
    formContent.put("description", description);
    if (request.containsKey("reason")) {
      formContent.put("reason", text(request, "reason", ""));
    }
    Object attachments = request.containsKey("attachments") ? request.get("attachments") : request.get("attachmentFiles");
    if (attachments != null) {
      formContent.put("attachmentFiles", attachments);
    }
    long id =
        feedbackRepository.create(
            ticketNo,
            accessToken,
            user == null ? null : user.id(),
            text(request, "reportUrl", text(request, "report_url", "")),
            reportType.wireValue(),
            writeJson(formContent),
            text(request, "email", user == null ? "" : user.email()),
            header(servletRequest, "User-Agent"),
            clientIp(servletRequest));
    feedbackRepository.insertMessage(
        id,
        FeedbackActorType.USER.name(),
        user == null ? null : user.id(),
        FeedbackMessageType.MESSAGE.name(),
        description,
        writeJsonArray(attachments),
        null,
        null);
    Map<String, Object> feedback = get(id);
    feedbackNotifier.notifyNewFeedback(feedback);
    feedbackMailer.sendNewFeedback(feedbackRepository.ownerEmail(), ticketNo, formContent);
    return feedback;
  }

  public Map<String, Object> getByTicket(String ticketNo) {
    return feedbackRepository.getByTicket(ticketNo);
  }

  public Map<String, Object> getByAccessToken(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      throw new BusinessException(40072, "Access token is required", HttpStatus.BAD_REQUEST);
    }
    return feedbackRepository.getByAccessToken(accessToken);
  }

  public PageResponse<Map<String, Object>> listMine(String email, Map<String, String> params) {
    UserAccount user = userRepository.findByEmail(email);
    return feedbackRepository.listByUserId(user.id(), params);
  }

  public PageResponse<Map<String, Object>> listAdmin(Map<String, String> params) {
    return feedbackRepository.listAdmin(params);
  }

  public Map<String, Object> get(long id) {
    return feedbackRepository.get(id);
  }

  @Transactional
  public Map<String, Object> addUserMessage(long id, Map<String, Object> request, Principal principal) {
    Map<String, Object> feedback = authorizeUserMessage(id, request, principal);
    FeedbackStatus current = FeedbackStatus.from(text(feedback, "status", "PENDING"));
    FeedbackStatus next = transitionPolicy.afterUserReply(current);
    String content = requiredText(request, "content");
    Object attachments = request.containsKey("attachments") ? request.get("attachments") : request.get("attachmentFiles");
    Long actorUserId = principal == null ? null : userRepository.findByEmail(principal.getName()).id();
    feedbackRepository.insertMessage(
        id,
        FeedbackActorType.USER.name(),
        actorUserId,
        FeedbackMessageType.MESSAGE.name(),
        content,
        writeJsonArray(attachments),
        null,
        null);
    feedbackRepository.touchUserReply(id);
    if (next != current) {
      feedbackRepository.updateStatus(id, next.name());
      feedbackRepository.insertMessage(
          id,
          FeedbackActorType.SYSTEM.name(),
          null,
          FeedbackMessageType.STATUS_CHANGE.name(),
          "用户已补充信息，工单回到处理中。",
          "[]",
          current.name(),
          next.name());
    }
    return get(id);
  }

  @Transactional
  public Map<String, Object> addAdminMessage(long id, Map<String, Object> request) {
    Map<String, Object> before = get(id);
    FeedbackStatus current = FeedbackStatus.from(text(before, "status", "PENDING"));
    FeedbackStatus next = current == FeedbackStatus.PENDING ? FeedbackStatus.IN_PROGRESS : current;
    if (next != current) {
      transitionPolicy.requireAllowed(current, next);
      feedbackRepository.updateStatus(id, next.name());
      feedbackRepository.insertMessage(
          id,
          FeedbackActorType.SYSTEM.name(),
          null,
          FeedbackMessageType.STATUS_CHANGE.name(),
          "管理员开始处理该工单。",
          "[]",
          current.name(),
          next.name());
    }
    String content = requiredText(request, "content", text(request, "admin_reply", ""));
    feedbackRepository.insertMessage(
        id,
        FeedbackActorType.ADMIN.name(),
        null,
        FeedbackMessageType.MESSAGE.name(),
        content,
        writeJsonArray(request.get("attachments")),
        null,
        null);
    feedbackRepository.updateAdminReply(id, content);
    Map<String, Object> feedback = get(id);
    notifySubmitter(feedback, "反馈有新的回复", content);
    return feedback;
  }

  @Transactional
  public Map<String, Object> updateAdminStatus(long id, Map<String, Object> request) {
    Map<String, Object> before = get(id);
    FeedbackStatus current = FeedbackStatus.from(text(before, "status", "PENDING"));
    FeedbackStatus target = feedbackStatus(text(request, "status", current.name()));
    transitionPolicy.requireAllowed(current, target);
    if (target != current) {
      feedbackRepository.updateStatus(id, target.name());
      feedbackRepository.insertMessage(
          id,
          FeedbackActorType.SYSTEM.name(),
          null,
          FeedbackMessageType.STATUS_CHANGE.name(),
          text(request, "content", "工单状态已更新为：" + target.label()),
          "[]",
          current.name(),
          target.name());
    }
    Map<String, Object> feedback = get(id);
    if (target != current) {
      notifySubmitter(feedback, "反馈状态已更新", "工单 " + feedback.get("ticket_no") + " 已更新为：" + target.label());
    }
    return feedback;
  }

  @Transactional
  public Map<String, Object> update(long id, Map<String, Object> request) {
    String reply = text(request, "admin_reply", "");
    Map<String, Object> feedback = updateAdminStatus(id, request);
    if (!reply.isBlank()) {
      feedback = addAdminMessage(id, Map.of("content", reply));
    }
    Map<String, Object> legacy = new LinkedHashMap<>(feedback);
    legacy.put("status", text(feedback, "status", "").toLowerCase());
    legacy.put("admin_reply", reply.isBlank() ? feedback.get("admin_reply") : reply);
    return legacy;
  }

  public void delete(long id) {
    feedbackRepository.delete(id);
  }

  private Map<String, Object> authorizeUserMessage(long id, Map<String, Object> request, Principal principal) {
    Map<String, Object> feedback = get(id);
    if (principal != null) {
      UserAccount user = userRepository.findByEmail(principal.getName());
      Object owner = feedback.get("user_id");
      if (owner instanceof Number number && number.longValue() == user.id()) {
        return feedback;
      }
    }
    String accessToken = text(request, "access_token", text(request, "accessToken", ""));
    if (!accessToken.isBlank() && accessToken.equals(text(feedback, "access_token", ""))) {
      return feedback;
    }
    throw new BusinessException(40372, "Feedback access denied", HttpStatus.FORBIDDEN);
  }

  private void notifySubmitter(Map<String, Object> feedback, String title, String content) {
    Object userId = feedback.get("user_id");
    if (userId instanceof Number number && number.longValue() > 0) {
      feedbackNotifier.notifyFeedbackUserUpdate(number.longValue(), feedback, title, content);
    }
    String recipient = text(feedback, "email", "");
    if (!recipient.isBlank()) {
      feedbackMailer.sendReply(feedback, recipient, content);
    }
  }

  private FeedbackType feedbackType(String value) {
    try {
      return FeedbackType.from(value);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(40001, "Invalid report type", HttpStatus.BAD_REQUEST);
    }
  }

  private FeedbackStatus feedbackStatus(String value) {
    try {
      return FeedbackStatus.from(value);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(40001, "Invalid feedback status", HttpStatus.BAD_REQUEST);
    }
  }

  private String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
  }

  private String header(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    return value == null ? "" : value;
  }

  private String requiredText(Map<String, Object> request, String key) {
    return requiredText(request, key, "");
  }

  private String requiredText(Map<String, Object> request, String key, String fallback) {
    String value = text(request, key, fallback).trim();
    if (value.isBlank()) {
      throw new BusinessException(40002, key + " is required", HttpStatus.BAD_REQUEST);
    }
    if (value.length() > 5000) {
      throw new BusinessException(40003, key + " is too long", HttpStatus.BAD_REQUEST);
    }
    return value;
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

  private String writeJsonArray(Object content) {
    try {
      return objectMapper.writeValueAsString(content == null ? java.util.List.of() : content);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid feedback attachment data", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
