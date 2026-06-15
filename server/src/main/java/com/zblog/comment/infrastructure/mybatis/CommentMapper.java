package com.zblog.comment.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {

  long countRootRows(@Param("targetType") String targetType, @Param("targetKey") String targetKey);

  long countAllPublicRows(@Param("targetType") String targetType, @Param("targetKey") String targetKey);

  List<Map<String, Object>> listRootRows(
      @Param("targetType") String targetType,
      @Param("targetKey") String targetKey,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> listInitialReplyRows(
      @Param("rootIds") List<Long> rootIds, @Param("limitPerRoot") int limitPerRoot);

  long countReplies(@Param("rootId") long rootId);

  List<Map<String, Object>> countRepliesForRoots(@Param("rootIds") List<Long> rootIds);

  List<Map<String, Object>> listReplyRows(
      @Param("rootId") long rootId, @Param("limit") int limit, @Param("offset") int offset);

  long countRootsBefore(
      @Param("targetType") String targetType,
      @Param("targetKey") String targetKey,
      @Param("createdAt") Object createdAt,
      @Param("id") long id);

  long countRepliesBefore(
      @Param("rootId") long rootId, @Param("createdAt") Object createdAt, @Param("id") long id);

  long countAdminRows(
      @Param("keyword") String keyword,
      @Param("status") Integer status,
      @Param("deleted") Boolean deleted,
      @Param("sub") Boolean sub,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  List<Map<String, Object>> listAdminRows(
      @Param("keyword") String keyword,
      @Param("status") Integer status,
      @Param("deleted") Boolean deleted,
      @Param("sub") Boolean sub,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("limit") int limit,
      @Param("offset") int offset);

  void insertComment(Map<String, Object> params);

  void insertImportedComment(Map<String, Object> params);

  void toggleStatus(@Param("id") long id);

  int delete(@Param("id") long id);

  List<Map<String, Object>> rowsById(@Param("id") long id);
}
