package com.zblog.taxonomy.infrastructure.mybatis;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaxonomyMapper {

  java.util.List<Map<String, Object>> listCategories(@Param("limit") int limit, @Param("offset") int offset);

  long countCategories();

  java.util.List<Map<String, Object>> categoryByIdOrSlug(@Param("idOrSlug") String idOrSlug);

  void insertCategory(Map<String, Object> params);

  void updateCategory(
      @Param("id") long id,
      @Param("name") String name,
      @Param("slug") String slug,
      @Param("description") String description,
      @Param("sort") int sort);

  void deleteCategory(@Param("id") long id);

  java.util.List<Map<String, Object>> listTags(@Param("limit") int limit, @Param("offset") int offset);

  long countTags();

  java.util.List<Map<String, Object>> tagByIdOrSlug(@Param("idOrSlug") String idOrSlug);

  void insertTag(Map<String, Object> params);

  void updateTag(
      @Param("id") long id,
      @Param("name") String name,
      @Param("slug") String slug,
      @Param("description") String description);

  void deleteTag(@Param("id") long id);
}
