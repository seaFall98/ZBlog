package com.zblog.notification.application.port;

import java.util.List;
import java.util.Map;

public interface NotificationRepository {

  long countAll();

  long countUnread();

  List<Map<String, Object>> list(int pageSize, int offset);

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

  void markRead(long id);

  int markAllRead();
}
