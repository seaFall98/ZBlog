package com.zblog.content.application.port;

import com.zblog.content.application.TaxonomyEnsureResult;

public interface ArticleImportSupportRepository {

  TaxonomyEnsureResult ensureCategory(String name, String slug);

  TaxonomyEnsureResult ensureTag(String name, String slug);

  boolean articleSlugExists(String slug);
}
