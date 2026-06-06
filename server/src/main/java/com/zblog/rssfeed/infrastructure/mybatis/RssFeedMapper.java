package com.zblog.rssfeed.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RssFeedMapper {

  long countAdmin(
      @Param("keyword") String keyword,
      @Param("friendId") Long friendId,
      @Param("read") Boolean read,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  long unreadCount();

  List<Map<String, Object>> listAdmin(
      @Param("keyword") String keyword,
      @Param("friendId") Long friendId,
      @Param("read") Boolean read,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> sourceRows();

  List<Long> articleIdsByFriendAndLink(@Param("friendId") long friendId, @Param("link") String link);

  void insertItem(Map<String, Object> params);

  void markSourceSuccess(@Param("friendId") long friendId);

  void markSourceFailed(@Param("friendId") long friendId, @Param("errorMessage") String errorMessage);

  void markRead(@Param("id") long id);

  int markAllRead();
}
