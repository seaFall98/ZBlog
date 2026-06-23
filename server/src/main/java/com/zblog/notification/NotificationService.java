package com.zblog.notification;

import com.zblog.common.exception.BusinessException;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.identity.domain.UserAccount;
import com.zblog.notification.application.port.NotificationRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final RedisUnreadNotificationCount unreadCount;

  public NotificationService(
      NotificationRepository notificationRepository, UserRepository userRepository, RedisUnreadNotificationCount unreadCount) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.unreadCount = unreadCount;
  }

  public Map<String, Object> list(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    long total = notificationRepository.countAll();
    long unread = notificationRepository.countUnread();
    List<Map<String, Object>> list = notificationRepository.list(pageSize, offset);
    return Map.of("list", list, "total", total, "page", page, "page_size", pageSize, "unread_count", unread);
  }

  public Map<String, Object> listAdmin(
      int page, int pageSize, String type, Boolean read, Boolean processed, String keyword) {
    int offset = Math.max(0, page - 1) * pageSize;
    long total = notificationRepository.countFiltered(type, read, processed, keyword);
    long unread = notificationRepository.countUnread();
    List<Map<String, Object>> list =
        notificationRepository.listFiltered(type, read, processed, keyword, pageSize, offset);
    return Map.of("list", list, "total", total, "page", page, "page_size", pageSize, "unread_count", unread);
  }

  public Map<String, Object> listForUser(String email, int page, int pageSize, boolean unreadOnly) {
    UserAccount user = userRepository.findByEmail(email);
    int offset = Math.max(0, page - 1) * pageSize;
    long total = notificationRepository.countByRecipient(user.id(), unreadOnly);
    long unread = unreadCount.get(user.id(), () -> notificationRepository.countUnreadByRecipient(user.id()));
    List<Map<String, Object>> list =
        notificationRepository.listByRecipient(user.id(), unreadOnly, pageSize, offset);
    return Map.of("list", list, "total", total, "page", page, "page_size", pageSize, "unread_count", unread);
  }

  public Map<String, Object> unreadCountForUser(String email) {
    UserAccount user = userRepository.findByEmail(email);
    return Map.of("unread_count", unreadCount.get(user.id(), () -> notificationRepository.countUnreadByRecipient(user.id())));
  }

  public Map<String, Object> createFeedbackNotification(Map<String, Object> feedback) {
    long targetId = ((Number) feedback.get("id")).longValue();
    String ticketNo = feedback.get("ticket_no").toString();
    String reportType = feedback.get("report_type").toString();
    String reportUrl = feedback.get("report_url").toString();
    Map<String, Object> data =
        Map.of(
            "ticket_no", ticketNo,
            "report_url", reportUrl,
            "report_type", reportType,
            "form_content", feedback.get("form_content"),
            "status", feedback.get("status"));
    long id =
        notificationRepository.create(
            "feedback_new",
            "新的反馈工单",
            "收到来自 " + feedback.get("email") + " 的反馈：" + ticketNo,
            "/feedback?ticket_no=" + ticketNo,
            data,
            targetId,
            "system");
    return notificationRepository.get(id);
  }

  public Map<String, Object> createFeedbackUserNotification(
      long recipientUserId, Map<String, Object> feedback, String title, String content) {
    long targetId = ((Number) feedback.get("id")).longValue();
    String ticketNo = feedback.get("ticket_no").toString();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("feedback_id", targetId);
    data.put("ticket_no", ticketNo);
    data.put("status", feedback.get("status"));
    data.put("status_label", feedback.get("status_label"));
    long id =
        notificationRepository.createForRecipient(
            recipientUserId,
            "feedback_update",
            title,
            content,
            "/feedback/mine?ticket=" + ticketNo,
            data,
            targetId,
            "feedback",
            ticketNo,
            null,
            "direct");
    unreadCount.invalidate(recipientUserId);
    return notificationRepository.get(id);
  }

  public Map<String, Object> createArticlePublishedNotification(long articleId, String title, String slug, long eventId) {
    if (notificationRepository.countArticlePublished(articleId) > 0) {
      return notificationRepository.latestArticlePublished(articleId);
    }
    long id =
        notificationRepository.create(
            "article_published",
            "文章已发布",
            "文章《" + title + "》已发布",
            "/articles/edit/" + articleId,
            Map.of("article_id", articleId, "slug", slug, "event_id", eventId),
            articleId,
            "mq");
    return notificationRepository.get(id);
  }

  public Map<String, Object> createFriendApplyNotification(Map<String, Object> friend) {
    long id = ((Number) friend.get("id")).longValue();
    String name = friend.get("name").toString();
    long notificationId =
        notificationRepository.create(
            "friend_apply",
            "新的友链申请",
            "网站《" + name + "》申请友链",
            "/friends?is_pending=true",
            Map.of("friend_id", id, "name", name),
            id,
            "direct");
    return notificationRepository.get(notificationId);
  }

  public Map<String, Object> createGuestbookRootCommentNotification(
      long commentId, long actorUserId, String actorNickname, String content) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("actor_user_id", actorUserId);
    data.put("actor_nickname", actorNickname);
    data.put("target_type", "page");
    data.put("target_key", "guestbook");
    data.put("comment_id", commentId);
    long id =
        notificationRepository.create(
            "comment_new",
            "新的留言",
            actorNickname + " 在留言板发布了新留言",
            "/guestbook?commentId=" + commentId,
            data,
            commentId,
            "direct");
    return notificationRepository.get(id);
  }

  public Map<String, Object> createCommentReplyNotification(
      long recipientUserId,
      long actorUserId,
      String actorNickname,
      String targetType,
      String targetKey,
      long commentId,
      long parentId,
      String content) {
    String link =
        switch (targetType) {
          case "moment" -> "/moments?momentId=" + targetKey + "&commentId=" + commentId;
          case "page" -> "guestbook".equals(targetKey)
              ? "/guestbook?commentId=" + commentId
              : "/" + targetKey + "?commentId=" + commentId;
          default -> "/posts/" + targetKey + "?commentId=" + commentId;
        };
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("actor_user_id", actorUserId);
    data.put("actor_nickname", actorNickname);
    data.put("target_type", targetType);
    data.put("target_key", targetKey);
    data.put("comment_id", commentId);
    data.put("parent_id", parentId);
    long id =
        notificationRepository.createForRecipient(
            recipientUserId,
            "comment_reply",
            actorNickname + " 回复了你的评论",
            content.length() > 120 ? content.substring(0, 120) + "..." : content,
            link,
            data,
            commentId,
            targetType,
            targetKey,
            commentId,
            "mq");
    unreadCount.invalidate(recipientUserId);
    return notificationRepository.get(id);
  }

  public Map<String, Object> markRead(long id) {
    notificationRepository.markRead(id);
    Map<String, Object> result = notificationRepository.get(id);
    Object recipient = result.get("recipient_user_id");
    if (recipient instanceof Number number && number.longValue() > 0) {
      unreadCount.invalidate(number.longValue());
    }
    return result;
  }

  public Map<String, Object> markReadForUser(String email, long id) {
    UserAccount user = userRepository.findByEmail(email);
    int affected = notificationRepository.markReadByRecipient(id, user.id());
    if (affected == 0) {
      throw new BusinessException(404, "Notification not found", HttpStatus.NOT_FOUND);
    }
    unreadCount.invalidate(user.id());
    return notificationRepository.getForRecipient(id, user.id());
  }

  @Transactional
  public Map<String, Object> markAllRead() {
    int affected = notificationRepository.markAllRead();
    return Map.of("affected", affected, "unread_count", 0);
  }

  public Map<String, Object> markProcessed(long id, boolean processed) {
    notificationRepository.markProcessed(id, processed);
    return notificationRepository.get(id);
  }

  @Transactional
  public Map<String, Object> markAllReadForUser(String email) {
    UserAccount user = userRepository.findByEmail(email);
    int affected = notificationRepository.markAllReadByRecipient(user.id());
    unreadCount.invalidate(user.id());
    return Map.of("affected", affected, "unread_count", 0);
  }
}
