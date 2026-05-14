package com.zblog2.taxonomy.application;

import com.zblog2.common.api.PageResponse;
import com.zblog2.common.util.Slugify;
import com.zblog2.taxonomy.infrastructure.JdbcTaxonomyRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TaxonomyService {

  private final JdbcTaxonomyRepository taxonomyRepository;

  public TaxonomyService(JdbcTaxonomyRepository taxonomyRepository) {
    this.taxonomyRepository = taxonomyRepository;
  }

  public PageResponse<Map<String, Object>> listCategories(int page, int pageSize) {
    return taxonomyRepository.listCategories(page, pageSize);
  }

  public Map<String, Object> getCategory(String idOrSlug) {
    return taxonomyRepository.getCategory(idOrSlug);
  }

  public Map<String, Object> createCategory(Map<String, Object> request) {
    String name = text(request, "name");
    String slug = textOrDefault(request, "slug", Slugify.from(name));
    String description = textOrDefault(request, "description", "");
    int sort = numberOrDefault(request, "sort", 0);
    return taxonomyRepository.createCategory(name, slug, description, sort);
  }

  public Map<String, Object> updateCategory(long id, Map<String, Object> request) {
    return taxonomyRepository.updateCategory(
        id,
        text(request, "name"),
        textOrDefault(request, "slug", Slugify.from(text(request, "name"))),
        textOrDefault(request, "description", ""),
        numberOrDefault(request, "sort", 0));
  }

  public void deleteCategory(long id) {
    taxonomyRepository.deleteCategory(id);
  }

  public PageResponse<Map<String, Object>> listTags(int page, int pageSize) {
    return taxonomyRepository.listTags(page, pageSize);
  }

  public Map<String, Object> getTag(String idOrSlug) {
    return taxonomyRepository.getTag(idOrSlug);
  }

  public Map<String, Object> createTag(Map<String, Object> request) {
    String name = text(request, "name");
    String slug = textOrDefault(request, "slug", Slugify.from(name));
    String description = textOrDefault(request, "description", "");
    return taxonomyRepository.createTag(name, slug, description);
  }

  public Map<String, Object> updateTag(long id, Map<String, Object> request) {
    return taxonomyRepository.updateTag(
        id,
        text(request, "name"),
        textOrDefault(request, "slug", Slugify.from(text(request, "name"))),
        textOrDefault(request, "description", ""));
  }

  public void deleteTag(long id) {
    taxonomyRepository.deleteTag(id);
  }

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString().trim();
  }

  private String textOrDefault(Map<String, Object> request, String key, String defaultValue) {
    String value = text(request, key);
    return value.isBlank() ? defaultValue : value;
  }

  private int numberOrDefault(Map<String, Object> request, String key, int defaultValue) {
    Object value = request.get(key);
    return value instanceof Number number ? number.intValue() : defaultValue;
  }
}
