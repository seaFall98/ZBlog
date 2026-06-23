package com.zblog.notification.application.port;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public interface NotificationRepository {

  long countAll();

  long countUnread();

  List<Map<String, Object>> list(int pageSize, int offset);

  long countFiltered(String type, Boolean read, Boolean processed, String keyword);

  List<Map<String, Object>> listFiltered(
      String type, Boolean read, Boolean processed, String keyword, int pageSize, int offset);

  long countByRecipient(long recipientUserId, boolean unreadOnly);

  long countUnreadByRecipient(long recipientUserId);

  List<Map<String, Object>> listByRecipient(long recipientUserId, boolean unreadOnly, int pageSize, int offset);

  Map<String, Object> get(long id);

  long countArticlePublished(long articleId);

  Map<String, Object> latestArticlePublished(long articleId);

  long create(
      String type,
      String title,
      String content,
      String link,
      Map<String, Object> data,
      Long targetId,
      String sender);

  long createForRecipient(
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
      String sender);

  void markRead(long id);

  int markAllRead();

  int markReadByRecipient(long id, long recipientUserId);

  Map<String, Object> getForRecipient(long id, long recipientUserId);

  int markAllReadByRecipient(long recipientUserId);

  void markProcessed(long id, boolean processed);

  int deleteReadOlderThan(LocalDateTime threshold);
}
