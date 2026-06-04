package com.zblog.search.application.port;

import com.zblog.search.domain.IndexResult;
import java.util.Map;

public interface SearchStatusRepository {

  Map<String, Object> status(String strategy, boolean elasticsearchEnabled, boolean fallbackToDb);

  void recordReindex(
      String strategy, boolean elasticsearchEnabled, boolean fallbackToDb, IndexResult result);

  void recordError(String strategy, boolean elasticsearchEnabled, boolean fallbackToDb, String message);
}
