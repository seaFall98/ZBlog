package com.zblog.search.config;

import com.zblog.search.application.port.SearchIndexer;
import com.zblog.search.application.port.SearchPort;
import com.zblog.search.infrastructure.elasticsearch.ElasticsearchSearchAdapter;
import com.zblog.search.infrastructure.jdbc.DbSearchAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchStrategyConfiguration {

  // 搜索策略的 composition root，业务层只依赖 port，不感知具体 adapter。
  @Bean
  SearchPort activeSearchPort(
      SearchProperties properties,
      DbSearchAdapter dbSearchAdapter,
      ElasticsearchSearchAdapter elasticsearchSearchAdapter) {
    return properties.elasticsearchEnabled() ? elasticsearchSearchAdapter : dbSearchAdapter;
  }

  @Bean
  SearchIndexer activeSearchIndexer(
      SearchProperties properties,
      DbSearchAdapter dbSearchAdapter,
      ElasticsearchSearchAdapter elasticsearchSearchAdapter) {
    return properties.elasticsearchEnabled() ? elasticsearchSearchAdapter : dbSearchAdapter;
  }
}
