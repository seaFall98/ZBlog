package com.zblog.content.infrastructure.mybatis;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleImportSupportMapper {

  List<Long> categoryIdsBySlug(@Param("slug") String slug);

  void insertCategory(Map<String, Object> params);

  List<Long> tagIdsBySlug(@Param("slug") String slug);

  void insertTag(Map<String, Object> params);

  boolean articleSlugExists(@Param("slug") String slug);
}
