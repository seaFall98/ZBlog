package com.zblog.taxonomy.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.taxonomy.application.port.TaxonomyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisTaxonomyRepository implements TaxonomyRepository {

  private final TaxonomyMapper taxonomyMapper;

  public MyBatisTaxonomyRepository(TaxonomyMapper taxonomyMapper) {
    this.taxonomyMapper = taxonomyMapper;
  }

  public PageResponse<Map<String, Object>> listCategories(int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    return new PageResponse<>(
        taxonomyMapper.listCategories(pageSize, offset),
        taxonomyMapper.countCategories(),
        page,
        pageSize);
  }

  public Map<String, Object> getCategory(String idOrSlug) {
    return one(taxonomyMapper.categoryByIdOrSlug(idOrSlug), "Category not found");
  }

  public Map<String, Object> createCategory(String name, String slug, String description, int sort) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("name", name);
    params.put("slug", slug);
    params.put("description", description);
    params.put("sort", sort);
    taxonomyMapper.insertCategory(params);
    return getCategory(String.valueOf(generatedId(params)));
  }

  public Map<String, Object> updateCategory(long id, String name, String slug, String description, int sort) {
    taxonomyMapper.updateCategory(id, name, slug, description, sort);
    return getCategory(String.valueOf(id));
  }

  public void deleteCategory(long id) {
    taxonomyMapper.deleteCategory(id);
  }

  public PageResponse<Map<String, Object>> listTags(int page, int pageSize) {
    int offset = (page - 1) * pageSize;
    return new PageResponse<>(taxonomyMapper.listTags(pageSize, offset), taxonomyMapper.countTags(), page, pageSize);
  }

  public Map<String, Object> getTag(String idOrSlug) {
    return one(taxonomyMapper.tagByIdOrSlug(idOrSlug), "Tag not found");
  }

  public Map<String, Object> createTag(String name, String slug, String description) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("name", name);
    params.put("slug", slug);
    params.put("description", description);
    taxonomyMapper.insertTag(params);
    return getTag(String.valueOf(generatedId(params)));
  }

  public Map<String, Object> updateTag(long id, String name, String slug, String description) {
    taxonomyMapper.updateTag(id, name, slug, description);
    return getTag(String.valueOf(id));
  }

  public void deleteTag(long id) {
    taxonomyMapper.deleteTag(id);
  }

  private Map<String, Object> one(List<Map<String, Object>> list, String message) {
    if (list.isEmpty()) {
      throw new BusinessException(404, message, HttpStatus.NOT_FOUND);
    }
    return list.getFirst();
  }

  private long generatedId(Map<String, Object> params) {
    Object id = params.get("id");
    if (id instanceof Number number) {
      return number.longValue();
    }
    throw new IllegalStateException("MyBatis did not return generated id");
  }
}
