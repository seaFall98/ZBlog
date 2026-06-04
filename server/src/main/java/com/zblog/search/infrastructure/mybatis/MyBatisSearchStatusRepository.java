package com.zblog.search.infrastructure.mybatis;

import com.zblog.search.application.port.SearchStatusRepository;
import com.zblog.search.domain.IndexResult;
import com.zblog.search.infrastructure.mybatis.SearchStatusMapper.SearchStatusRow;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisSearchStatusRepository implements SearchStatusRepository {

  private final SearchStatusMapper searchStatusMapper;

  public MyBatisSearchStatusRepository(SearchStatusMapper searchStatusMapper) {
    this.searchStatusMapper = searchStatusMapper;
  }

  @Override
  public Map<String, Object> status(String strategy, boolean elasticsearchEnabled, boolean fallbackToDb) {
    ensureRow(strategy, elasticsearchEnabled, fallbackToDb);
    return mapRow(searchStatusMapper.status(), strategy, elasticsearchEnabled, fallbackToDb);
  }

  @Override
  public void recordReindex(
      String strategy, boolean elasticsearchEnabled, boolean fallbackToDb, IndexResult result) {
    ensureRow(strategy, elasticsearchEnabled, fallbackToDb);
    searchStatusMapper.recordReindex(
        strategy, elasticsearchEnabled, fallbackToDb, result.indexed(), result.deleted(), result.failed());
  }

  @Override
  public void recordError(String strategy, boolean elasticsearchEnabled, boolean fallbackToDb, String message) {
    ensureRow(strategy, elasticsearchEnabled, fallbackToDb);
    searchStatusMapper.recordError(strategy, elasticsearchEnabled, fallbackToDb, message);
  }

  private void ensureRow(String strategy, boolean elasticsearchEnabled, boolean fallbackToDb) {
    searchStatusMapper.ensureRow(strategy, elasticsearchEnabled, fallbackToDb);
  }

  private Map<String, Object> mapRow(
      SearchStatusRow row, String strategy, boolean elasticsearchEnabled, boolean fallbackToDb) {
    Map<String, Object> mapped = new LinkedHashMap<>();
    mapped.put("strategy", strategy);
    mapped.put("elasticsearch_enabled", elasticsearchEnabled);
    mapped.put("fallback_to_db", fallbackToDb);
    mapped.put("last_reindex", timestamp(row.lastReindexAt()));
    mapped.put("last_reindex_indexed", row.lastReindexIndexed());
    mapped.put("last_reindex_deleted", row.lastReindexDeleted());
    mapped.put("last_reindex_failed", row.lastReindexFailed());
    mapped.put("last_error", row.lastError());
    mapped.put("last_error_at", timestamp(row.lastErrorAt()));
    return mapped;
  }

  private Object timestamp(LocalDateTime value) {
    return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
  }
}
