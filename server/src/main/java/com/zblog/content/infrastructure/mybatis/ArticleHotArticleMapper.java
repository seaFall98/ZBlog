package com.zblog.content.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleHotArticleMapper {

  List<Map<String, Object>> hotPublished(@Param("limit") int limit);

  List<Map<String, Object>> findPublishedByIds(@Param("ids") List<Long> ids);

  List<Map<String, Object>> findTagsByArticleIds(@Param("articleIds") List<Long> articleIds);
}
