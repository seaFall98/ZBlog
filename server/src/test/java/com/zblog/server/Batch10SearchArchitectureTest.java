package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class Batch10SearchArchitectureTest {

  @Test
  void searchModuleUsesLayeredPackagesInsteadOfFlatFeatureBucket() throws Exception {
    assertThat(Class.forName("com.zblog.search.application.SearchService")).isNotNull();
    assertThat(Class.forName("com.zblog.search.application.SearchStatusService")).isNotNull();
    assertThat(Class.forName("com.zblog.search.application.port.SearchPort")).isNotNull();
    assertThat(Class.forName("com.zblog.search.application.port.SearchIndexer")).isNotNull();
    assertThat(Class.forName("com.zblog.search.controller.SearchAdminController")).isNotNull();
    assertThat(Class.forName("com.zblog.search.domain.SearchDocument")).isNotNull();
    assertThat(Class.forName("com.zblog.search.domain.SearchStrategy")).isNotNull();
    assertThat(Class.forName("com.zblog.search.infrastructure.jdbc.DbSearchAdapter")).isNotNull();
    assertThat(Class.forName("com.zblog.search.infrastructure.elasticsearch.ElasticsearchSearchAdapter")).isNotNull();
    assertThat(Class.forName("com.zblog.search.infrastructure.messaging.SearchIndexEventConsumer")).isNotNull();
  }

  @Test
  void searchServiceDependsOnPortsAndResolvedStrategyNotConcreteAdapters() throws Exception {
    Class<?> service = Class.forName("com.zblog.search.application.SearchService");
    Set<String> fieldTypes =
        Arrays.stream(service.getDeclaredFields()).map(Field::getType).map(Class::getName).collect(Collectors.toSet());

    assertThat(fieldTypes).contains("com.zblog.search.application.port.SearchPort");
    assertThat(fieldTypes).contains("com.zblog.search.application.port.SearchIndexer");
    assertThat(fieldTypes).doesNotContain("com.zblog.search.infrastructure.jdbc.DbSearchAdapter");
    assertThat(fieldTypes).doesNotContain("com.zblog.search.infrastructure.elasticsearch.ElasticsearchSearchAdapter");
  }

  @Test
  void searchPropertiesUseEnumStrategyAndNestedElasticsearchConfig() throws Exception {
    Class<?> properties = Class.forName("com.zblog.search.config.SearchProperties");

    assertThat(properties.getDeclaredField("strategy").getType().getName())
        .isEqualTo("com.zblog.search.domain.SearchStrategy");
    assertThat(properties.getDeclaredField("elasticsearch").getType().getName())
        .isEqualTo("com.zblog.search.config.SearchProperties$Elasticsearch");
  }
}
