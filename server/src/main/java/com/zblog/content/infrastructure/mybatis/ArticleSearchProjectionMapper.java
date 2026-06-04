package com.zblog.content.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleSearchProjectionMapper {

  List<Map<String, Object>> findSearchProjectionById(@Param("articleId") long articleId);

  List<Map<String, Object>> listSearchProjections();

  long countSearchPublic(@Param("keyword") String keyword);

  List<Map<String, Object>> searchPublic(
      @Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

  List<Map<String, Object>> findTagsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
