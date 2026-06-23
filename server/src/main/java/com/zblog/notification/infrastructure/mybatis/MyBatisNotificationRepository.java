package com.zblog.notification.infrastructure.mybatis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.exception.BusinessException;
import com.zblog.notification.application.port.NotificationRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisNotificationRepository implements NotificationRepository {

  private final NotificationMapper notificationMapper;
  private final ObjectMapper objectMapper;

  public MyBatisNotificationRepository(NotificationMapper notificationMapper, ObjectMapper objectMapper) {
    this.notificationMapper = notificationMapper;
    this.objectMapper = objectMapper;
  }

  public long countAll() {
    return notificationMapper.countAll();
  }

  public long countUnread() {
    return notificationMapper.countUnread();
  }

  public List<Map<String, Object>> list(int pageSize, int offset) {
    return notificationMapper.listRows(pageSize, offset).stream().map(this::mapRow).toList();
  }

  public long countFiltered(String type, Boolean read, Boolean processed, String keyword) {
    return notificationMapper.countFiltered(blankToNull(type), read, processed, blankToNull(keyword));
  }

  public List<Map<String, Object>> listFiltered(
      String type, Boolean read, Boolean processed, String keyword, int pageSize, int offset) {
    return notificationMapper
        .listFilteredRows(blankToNull(type), read, processed, blankToNull(keyword), pageSize, offset)
        .stream()
        .map(this::mapRow)
        .toList();
  }

  public long countByRecipient(long recipientUserId, boolean unreadOnly) {
    return notificationMapper.countByRecipient(recipientUserId, unreadOnly);
  }

  public long countUnreadByRecipient(long recipientUserId) {
    return notificationMapper.countUnreadByRecipient(recipientUserId);
  }

  public List<Map<String, Object>> listByRecipient(long recipientUserId, boolean unreadOnly, int pageSize, int offset) {
    return notificationMapper.listRowsByRecipient(recipientUserId, unreadOnly, pageSize, offset).stream()
        .map(this::mapRow)
        .toList();
  }

  public Map<String, Object> get(long id) {
    return notificationMapper.rowsById(id).stream()
        .findFirst()
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(404, "Notification not found", HttpStatus.NOT_FOUND));
  }

  public long countArticlePublished(long articleId) {
    return notificationMapper.countArticlePublished(articleId);
  }

  public Map<String, Object> latestArticlePublished(long articleId) {
    return mapRow(notificationMapper.latestArticlePublished(articleId).getFirst());
  }

  public long create(
      String type,
      String title,
      String content,
      String link,
      Map<String, Object> data,
      Long targetId,
      String sender) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("type", type);
    params.put("title", title);
    params.put("content", content);
    params.put("link", link);
    params.put("data", writeJson(data));
    params.put("targetId", targetId);
    params.put("sender", sender);
    params.put("recipientUserId", null);
    params.put("targetType", null);
    params.put("targetKey", null);
    params.put("targetCommentId", null);
    notificationMapper.insertNotification(params);
    return ((Number) params.get("id")).longValue();
  }

  public long createForRecipient(
      long recipientUserId,
      String type,
      String title,
      String content,
      String link,
      Map<String, Object> data,
      Long targetId,
      String targetType,
      String targetKey,
      Long targetCommentId,
      String sender) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("type", type);
    params.put("title", title);
    params.put("content", content);
    params.put("link", link);
    params.put("data", writeJson(data));
    params.put("targetId", targetId);
    params.put("sender", sender);
    params.put("recipientUserId", recipientUserId);
    params.put("targetType", targetType);
    params.put("targetKey", targetKey);
    params.put("targetCommentId", targetCommentId);
    notificationMapper.insertNotification(params);
    return ((Number) params.get("id")).longValue();
  }

  public void markRead(long id) {
    notificationMapper.markRead(id);
  }

  public int markAllRead() {
    return notificationMapper.markAllRead();
  }

  public int markReadByRecipient(long id, long recipientUserId) {
    return notificationMapper.markReadByRecipient(id, recipientUserId);
  }

  public Map<String, Object> getForRecipient(long id, long recipientUserId) {
    return notificationMapper.rowsByIdForRecipient(id, recipientUserId).stream()
        .findFirst()
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(404, "Notification not found", HttpStatus.NOT_FOUND));
  }

  public int markAllReadByRecipient(long recipientUserId) {
    return notificationMapper.markAllReadByRecipient(recipientUserId);
  }

  public void markProcessed(long id, boolean processed) {
    notificationMapper.markProcessed(id, processed);
  }

  public int deleteReadOlderThan(LocalDateTime threshold) {
    return notificationMapper.deleteReadOlderThan(threshold);
  }

  private Map<String, Object> mapRow(Map<String, Object> source) {
    Map<String, Object> row = new LinkedHashMap<>();
    String type = string(source.get("type"));
    row.put("id", source.get("id"));
    row.put("type", type);
    row.put("type_text", typeText(type));
    row.put("title", source.get("title"));
    row.put("content", source.get("content"));
    row.put("link", source.get("link"));
    row.put("data", readJson(string(source.get("data"))));
    row.put("target_id", source.get("target_id"));
    row.put("recipient_user_id", source.get("recipient_user_id"));
    row.put("target_type", source.get("target_type"));
    row.put("target_key", source.get("target_key"));
    row.put("target_comment_id", source.get("target_comment_id"));
    row.put("is_read", source.get("is_read"));
    row.put("read_at", source.get("read_at"));
    row.put("is_processed", source.get("is_processed"));
    row.put("processed_at", source.get("processed_at"));
    row.put("created_at", source.get("created_at"));
    row.put("sender", source.get("sender"));
    return row;
  }

  private String typeText(String type) {
    return switch (type) {
      case "feedback_new" -> "反馈投诉";
      case "comment_new" -> "新评论";
      case "comment_reply" -> "评论回复";
      case "friend_apply" -> "友链申请";
      case "article_published" -> "文章发布";
      default -> "系统通知";
    };
  }

  private Map<String, Object> readJson(String value) {
    try {
      return objectMapper.readValue(value, new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid notification data", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String writeJson(Map<String, Object> value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "Invalid notification data", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
