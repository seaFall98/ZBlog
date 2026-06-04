package com.zblog.search.application;

import com.zblog.search.application.port.SearchStatusRepository;
import com.zblog.search.config.SearchProperties;
import com.zblog.search.domain.IndexResult;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchStatusService {

  private final SearchStatusRepository searchStatusRepository;
  private final SearchProperties properties;

  public SearchStatusService(SearchStatusRepository searchStatusRepository, SearchProperties properties) {
    this.searchStatusRepository = searchStatusRepository;
    this.properties = properties;
  }

  public Map<String, Object> status() {
    return searchStatusRepository.status(
        strategy(), properties.elasticsearchEnabled(), properties.isFallbackToDb());
  }

  public void recordReindex(IndexResult result) {
    searchStatusRepository.recordReindex(
        strategy(), properties.elasticsearchEnabled(), properties.isFallbackToDb(), result);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordError(String message) {
    searchStatusRepository.recordError(
        strategy(), properties.elasticsearchEnabled(), properties.isFallbackToDb(), message);
  }

  private String strategy() {
    return properties.strategyName();
  }
}
