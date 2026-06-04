package com.zblog.search.config;

import com.zblog.search.domain.SearchStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zblog.search")
public class SearchProperties {

  private SearchStrategy strategy = SearchStrategy.DB;
  private boolean fallbackToDb = true;
  private Elasticsearch elasticsearch = new Elasticsearch();

  public SearchStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(SearchStrategy strategy) {
    this.strategy = strategy;
  }

  public boolean isFallbackToDb() {
    return fallbackToDb;
  }

  public void setFallbackToDb(boolean fallbackToDb) {
    this.fallbackToDb = fallbackToDb;
  }

  public Elasticsearch getElasticsearch() {
    return elasticsearch;
  }

  public void setElasticsearch(Elasticsearch elasticsearch) {
    this.elasticsearch = elasticsearch;
  }

  public String strategyName() {
    return strategy.value();
  }

  public boolean elasticsearchEnabled() {
    return strategy == SearchStrategy.ELASTICSEARCH;
  }

  public static class Elasticsearch {
    private String url = "http://localhost:9200";
    private String indexName = "zblog-articles";

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getIndexName() {
      return indexName;
    }

    public void setIndexName(String indexName) {
      this.indexName = indexName;
    }
  }
}
