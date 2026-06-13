package com.zblog.notification;

import com.zblog.notification.application.port.NotificationRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;

  public NotificationService(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  public Map<String, Object> list(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    long total = notificationRepository.countAll();
    long unread = notificationRepository.countUnread();
    List<Map<String, Object>> list = notificationRepository.list(pageSize, offset);
    return Map.of("list", list, "total", total, "page", page, "page_size", pageSize, "unread_count", unread);
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

  public Map<String, Object> markRead(long id) {
    notificationRepository.markRead(id);
    return notificationRepository.get(id);
  }

  @Transactional
  public Map<String, Object> markAllRead() {
    int affected = notificationRepository.markAllRead();
    return Map.of("affected", affected, "unread_count", 0);
  }
}
