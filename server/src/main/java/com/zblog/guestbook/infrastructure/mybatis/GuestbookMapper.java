package com.zblog.guestbook.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GuestbookMapper {

  void insertMessage(Map<String, Object> params);

  long countPublic();

  List<Map<String, Object>> listPublic(@Param("limit") int limit, @Param("offset") int offset);

  long countAdmin(
      @Param("keyword") String keyword,
      @Param("status") String status,
      @Param("pinned") Boolean pinned,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  List<Map<String, Object>> listAdmin(
      @Param("keyword") String keyword,
      @Param("status") String status,
      @Param("pinned") Boolean pinned,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("limit") int limit,
      @Param("offset") int offset);

  void updateStatus(@Param("id") long id, @Param("status") String status, @Param("adminNote") String adminNote);

  void updatePinned(@Param("id") long id, @Param("pinned") boolean pinned);

  void delete(@Param("id") long id);

  List<Map<String, Object>> rowsById(@Param("id") long id);
}
