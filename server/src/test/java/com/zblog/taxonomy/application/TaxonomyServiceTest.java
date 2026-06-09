package com.zblog.taxonomy.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.common.api.PageResponse;
import com.zblog.taxonomy.application.port.TaxonomyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TaxonomyServiceTest {

  @Test
  void createCategoryPassesCoverUrlToRepository() {
    FakeTaxonomyRepository repository = new FakeTaxonomyRepository();
    TaxonomyService service = new TaxonomyService(repository);

    service.createCategory(Map.of(
        "name", "写作",
        "slug", "writing",
        "description", "写作说明",
        "cover_url", "https://example.com/writing.jpg",
        "sort", 10));

    assertThat(repository.lastCoverUrl).isEqualTo("https://example.com/writing.jpg");
  }

  @Test
  void createCategoryAcceptsCamelCaseCoverUrl() {
    FakeTaxonomyRepository repository = new FakeTaxonomyRepository();
    TaxonomyService service = new TaxonomyService(repository);

    service.createCategory(Map.of(
        "name", "生活",
        "coverUrl", "https://example.com/life.jpg"));

    assertThat(repository.lastCoverUrl).isEqualTo("https://example.com/life.jpg");
  }

  @Test
  void updateCategoryPassesCoverUrlToRepository() {
    FakeTaxonomyRepository repository = new FakeTaxonomyRepository();
    TaxonomyService service = new TaxonomyService(repository);

    service.updateCategory(9L, Map.of(
        "name", "阅读",
        "slug", "reading",
        "description", "阅读说明",
        "cover_url", "https://example.com/reading.jpg",
        "sort", 20));

    assertThat(repository.lastCoverUrl).isEqualTo("https://example.com/reading.jpg");
  }

  private static final class FakeTaxonomyRepository implements TaxonomyRepository {
    String lastCoverUrl;

    @Override
    public PageResponse<Map<String, Object>> listCategories(int page, int pageSize) {
      return new PageResponse<>(List.of(), 0, page, pageSize);
    }

    @Override
    public Map<String, Object> getCategory(String idOrSlug) {
      return Map.of();
    }

    @Override
    public Map<String, Object> createCategory(String name, String slug, String description, int sort, String coverUrl) {
      lastCoverUrl = coverUrl;
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("cover_url", coverUrl);
      return result;
    }

    @Override
    public Map<String, Object> updateCategory(long id, String name, String slug, String description, int sort, String coverUrl) {
      lastCoverUrl = coverUrl;
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("cover_url", coverUrl);
      return result;
    }

    @Override
    public void deleteCategory(long id) {
    }

    @Override
    public PageResponse<Map<String, Object>> listTags(int page, int pageSize) {
      return new PageResponse<>(List.of(), 0, page, pageSize);
    }

    @Override
    public Map<String, Object> getTag(String idOrSlug) {
      return Map.of();
    }

    @Override
    public Map<String, Object> createTag(String name, String slug, String description) {
      return Map.of();
    }

    @Override
    public Map<String, Object> updateTag(long id, String name, String slug, String description) {
      return Map.of();
    }

    @Override
    public void deleteTag(long id) {
    }
  }
}
