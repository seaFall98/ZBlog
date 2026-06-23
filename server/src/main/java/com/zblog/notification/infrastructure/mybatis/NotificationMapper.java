package com.zblog.notification.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

  long countAll();

  long countUnread();

  List<Map<String, Object>> listRows(@Param("limit") int limit, @Param("offset") int offset);

  long countFiltered(
      @Param("type") String type,
      @Param("read") Boolean read,
      @Param("processed") Boolean processed,
      @Param("keyword") String keyword);

  List<Map<String, Object>> listFilteredRows(
      @Param("type") String type,
      @Param("read") Boolean read,
      @Param("processed") Boolean processed,
      @Param("keyword") String keyword,
      @Param("limit") int limit,
      @Param("offset") int offset);

  long countByRecipient(@Param("recipientUserId") long recipientUserId, @Param("unreadOnly") boolean unreadOnly);

  long countUnreadByRecipient(@Param("recipientUserId") long recipientUserId);

  List<Map<String, Object>> listRowsByRecipient(
      @Param("recipientUserId") long recipientUserId,
      @Param("unreadOnly") boolean unreadOnly,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> rowsById(@Param("id") long id);

  long countArticlePublished(@Param("articleId") long articleId);

  List<Map<String, Object>> latestArticlePublished(@Param("articleId") long articleId);

  void insertNotification(Map<String, Object> params);

  void markRead(@Param("id") long id);

  int markAllRead();

  int markReadByRecipient(@Param("id") long id, @Param("recipientUserId") long recipientUserId);

  List<Map<String, Object>> rowsByIdForRecipient(@Param("id") long id, @Param("recipientUserId") long recipientUserId);

  int markAllReadByRecipient(@Param("recipientUserId") long recipientUserId);

  void markProcessed(@Param("id") long id, @Param("processed") boolean processed);

  int deleteReadOlderThan(@Param("threshold") LocalDateTime threshold);
}
