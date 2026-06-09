package com.zblog.taxonomy.application.port;

import com.zblog.common.api.PageResponse;
import java.util.Map;

public interface TaxonomyRepository {

  PageResponse<Map<String, Object>> listCategories(int page, int pageSize);

  Map<String, Object> getCategory(String idOrSlug);

  Map<String, Object> createCategory(String name, String slug, String description, int sort, String coverUrl);

  Map<String, Object> updateCategory(long id, String name, String slug, String description, int sort, String coverUrl);

  void deleteCategory(long id);

  PageResponse<Map<String, Object>> listTags(int page, int pageSize);

  Map<String, Object> getTag(String idOrSlug);

  Map<String, Object> createTag(String name, String slug, String description);

  Map<String, Object> updateTag(long id, String name, String slug, String description);

  void deleteTag(long id);
}
