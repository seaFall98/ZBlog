package com.zblog.search.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.search.application.port.ArticleSearchSource;
import com.zblog.search.application.port.SearchIndexer;
import com.zblog.search.application.port.SearchPort;
import com.zblog.search.config.SearchProperties;
import com.zblog.search.domain.IndexResult;
import com.zblog.search.domain.SearchDocument;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SearchService implements SearchPort, SearchIndexer {

  // 搜索门面同时服务查询入口和索引消费者，隐藏 DB/Elasticsearch 的具体策略。
  private final SearchProperties properties;
  private final SearchPort searchPort;
  private final SearchIndexer searchIndexer;
  private final SearchPort fallbackSearchPort;
  private final ArticleSearchSource articleSearchSource;
  private final SearchStatusService statusService;

  public SearchService(
      SearchProperties properties,
      @Qualifier("activeSearchPort") SearchPort searchPort,
      @Qualifier("activeSearchIndexer") SearchIndexer searchIndexer,
      @Qualifier("dbSearchAdapter") SearchPort fallbackSearchPort,
      ArticleSearchSource articleSearchSource,
      SearchStatusService statusService) {
    this.properties = properties;
    this.searchPort = searchPort;
    this.searchIndexer = searchIndexer;
    this.fallbackSearchPort = fallbackSearchPort;
    this.articleSearchSource = articleSearchSource;
    this.statusService = statusService;
  }

  @Override
  public PageResponse<Map<String, Object>> search(String keyword, int page, int pageSize) {
    try {
      return searchPort.search(keyword, page, pageSize);
    } catch (RuntimeException exception) {
      statusService.recordError(exception.getMessage());
      if (properties.elasticsearchEnabled() && properties.isFallbackToDb()) {
        // ES 不可用时回退 DB 搜索，保证公开查询优先可用。
        return fallbackSearchPort.search(keyword, page, pageSize);
      }
      throw new BusinessException(50301, "Search backend unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  @Override
  public void upsert(SearchDocument document) {
    searchIndexer.upsert(document);
  }

  @Override
  public void delete(long articleId) {
    searchIndexer.delete(articleId);
  }

  @Override
  public IndexResult reindex(List<SearchDocument> documents) {
    IndexResult result = searchIndexer.reindex(documents);
    statusService.recordReindex(result);
    return result;
  }

  public Map<String, Object> reindexAllPublished() {
    // 重建索引以已发布文章 read-model 为源，避免草稿或后台字段进入公开搜索。
    List<SearchDocument> documents = articleSearchSource.publishedSearchDocuments();
    IndexResult result = reindex(documents);
    return Map.of(
        "strategy",
        properties.strategyName(),
        "indexed",
        result.indexed(),
        "deleted",
        result.deleted(),
        "failed",
        result.failed());
  }

  public Map<String, Object> status() {
    return statusService.status();
  }
}
