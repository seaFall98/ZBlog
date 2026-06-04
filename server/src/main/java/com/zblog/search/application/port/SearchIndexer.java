package com.zblog.search.application.port;

import com.zblog.search.domain.IndexResult;
import com.zblog.search.domain.SearchDocument;
import java.util.List;

public interface SearchIndexer {
  void upsert(SearchDocument document);

  void delete(long articleId);

  IndexResult reindex(List<SearchDocument> documents);
}
