package com.zblog.search.infrastructure.jdbc;

import com.zblog.common.api.PageResponse;
import com.zblog.search.application.port.ArticleSearchSource;
import com.zblog.search.application.port.SearchIndexer;
import com.zblog.search.application.port.SearchPort;
import com.zblog.search.domain.IndexResult;
import com.zblog.search.domain.SearchDocument;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DbSearchAdapter implements SearchPort, SearchIndexer {

  // DB 搜索既是默认策略，也是 ES 不可用时的 source-of-truth fallback。
  private final ArticleSearchSource articleSearchSource;

  public DbSearchAdapter(ArticleSearchSource articleSearchSource) {
    this.articleSearchSource = articleSearchSource;
  }

  @Override
  public PageResponse<Map<String, Object>> search(String keyword, int page, int pageSize) {
    return articleSearchSource.searchPublic(keyword, page, pageSize);
  }

  @Override
  public void upsert(SearchDocument document) {
    // PostgreSQL is the source of truth, so DB indexing is a no-op.
  }

  @Override
  public void delete(long articleId) {
    // PostgreSQL is the source of truth, so DB index deletion is a no-op.
  }

  @Override
  public IndexResult reindex(List<SearchDocument> documents) {
    return new IndexResult(documents.size(), 0, 0);
  }
}
