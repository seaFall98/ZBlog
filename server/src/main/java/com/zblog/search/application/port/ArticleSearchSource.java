package com.zblog.search.application.port;

import com.zblog.common.api.PageResponse;
import com.zblog.search.domain.SearchDocument;
import java.util.List;
import java.util.Map;

public interface ArticleSearchSource {

  PageResponse<Map<String, Object>> searchPublic(String keyword, int page, int pageSize);

  List<SearchDocument> publishedSearchDocuments();
}
