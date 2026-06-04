package com.zblog.content.infrastructure.mybatis;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleAdminQueryMapper {

  long countAdmin(
      @Param("keyword") String keyword,
      @Param("publishedStatus") String publishedStatus,
      @Param("categoryId") Long categoryId,
      @Param("tagIds") List<Long> tagIds,
      @Param("location") String location,
      @Param("top") Boolean top,
      @Param("essence") Boolean essence,
      @Param("outdated") Boolean outdated,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  List<Map<String, Object>> listAdmin(
      @Param("keyword") String keyword,
      @Param("publishedStatus") String publishedStatus,
      @Param("categoryId") Long categoryId,
      @Param("tagIds") List<Long> tagIds,
      @Param("location") String location,
      @Param("top") Boolean top,
      @Param("essence") Boolean essence,
      @Param("outdated") Boolean outdated,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> findAdminById(@Param("id") long id);

  List<Map<String, Object>> findTagsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
