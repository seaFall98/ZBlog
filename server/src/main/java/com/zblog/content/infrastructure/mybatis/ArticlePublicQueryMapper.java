package com.zblog.content.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticlePublicQueryMapper {

  long countPublic(
      @Param("category") String category,
      @Param("tag") String tag,
      @Param("year") Integer year,
      @Param("month") Integer month);

  List<Map<String, Object>> listPublic(
      @Param("category") String category,
      @Param("tag") String tag,
      @Param("year") Integer year,
      @Param("month") Integer month,
      @Param("limit") int limit,
      @Param("offset") int offset);

  List<Map<String, Object>> findPublicBySlug(@Param("slug") String slug);

  String findFirstPublishedSlug();

  long countSearchPublic(@Param("keyword") String keyword);

  List<Map<String, Object>> searchPublic(
      @Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> findTagsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
