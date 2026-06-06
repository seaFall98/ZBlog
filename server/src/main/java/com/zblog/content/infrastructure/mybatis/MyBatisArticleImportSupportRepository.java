package com.zblog.content.infrastructure.mybatis;

import com.zblog.content.application.TaxonomyEnsureResult;
import com.zblog.content.application.port.ArticleImportSupportRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisArticleImportSupportRepository implements ArticleImportSupportRepository {

  private final ArticleImportSupportMapper articleImportSupportMapper;

  public MyBatisArticleImportSupportRepository(ArticleImportSupportMapper articleImportSupportMapper) {
    this.articleImportSupportMapper = articleImportSupportMapper;
  }

  public TaxonomyEnsureResult ensureCategory(String name, String slug) {
    List<Long> existing = articleImportSupportMapper.categoryIdsBySlug(slug);
    if (!existing.isEmpty()) {
      return new TaxonomyEnsureResult(existing.getFirst(), false);
    }
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("name", name);
    params.put("slug", slug);
    articleImportSupportMapper.insertCategory(params);
    return new TaxonomyEnsureResult(generatedId(params), true);
  }

  public TaxonomyEnsureResult ensureTag(String name, String slug) {
    List<Long> existing = articleImportSupportMapper.tagIdsBySlug(slug);
    if (!existing.isEmpty()) {
      return new TaxonomyEnsureResult(existing.getFirst(), false);
    }
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("name", name);
    params.put("slug", slug);
    articleImportSupportMapper.insertTag(params);
    return new TaxonomyEnsureResult(generatedId(params), true);
  }

  public boolean articleSlugExists(String slug) {
    return articleImportSupportMapper.articleSlugExists(slug);
  }

  private long generatedId(Map<String, Object> params) {
    Object id = params.get("id");
    if (id instanceof Number number) {
      return number.longValue();
    }
    throw new IllegalStateException("MyBatis did not return generated id");
  }
}
