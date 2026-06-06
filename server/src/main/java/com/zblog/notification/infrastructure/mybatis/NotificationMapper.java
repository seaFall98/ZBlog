package com.zblog.notification.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

  long countAll();

  long countUnread();

  List<Map<String, Object>> listRows(@Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> rowsById(@Param("id") long id);

  long countArticlePublished(@Param("articleId") long articleId);

  List<Map<String, Object>> latestArticlePublished(@Param("articleId") long articleId);

  void insertNotification(Map<String, Object> params);

  void markRead(@Param("id") long id);

  int markAllRead();
}
