package com.zblog.comment.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper {

  List<Map<String, Object>> listPublicRows(
      @Param("targetType") String targetType, @Param("targetKey") String targetKey);

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

  void delete(@Param("id") long id);

  List<Map<String, Object>> rowsById(@Param("id") long id);
}
