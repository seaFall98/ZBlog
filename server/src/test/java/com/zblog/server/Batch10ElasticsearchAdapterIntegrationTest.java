package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.search.config.SearchProperties;
import com.zblog.search.domain.IndexResult;
import com.zblog.search.domain.SearchDocument;
import com.zblog.search.domain.SearchStrategy;
import com.zblog.search.infrastructure.elasticsearch.ElasticsearchSearchAdapter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "ZBLOG_TEST_ELASTICSEARCH_URL", matches = ".+")
class Batch10ElasticsearchAdapterIntegrationTest {

  private final String baseUrl = System.getenv("ZBLOG_TEST_ELASTICSEARCH_URL").replaceAll("/$", "");
  private final String indexName = "zblog-batch10-it-" + System.currentTimeMillis();
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  private final ElasticsearchSearchAdapter adapter = new ElasticsearchSearchAdapter(properties(), new ObjectMapper());

  @AfterEach
  void deleteIndex() throws Exception {
    request("DELETE", "/" + indexName);
  }

  @Test
  void realElasticsearchAdapterCanUpsertSearchDeleteAndReindex() throws Exception {
    adapter.upsert(
        new SearchDocument(
            101,
            "Batch 10 ES Adapter",
            "batch10-es-adapter",
            "summary batch10-real-es-keyword",
            "body batch10-real-es-keyword",
            "2026-05-18T00:00:00Z"));
    refresh();

    List<?> firstResults = adapter.search("batch10-real-es-keyword", 1, 10).list();
    assertThat(firstResults).anySatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo("batch10-es-adapter"));

    adapter.delete(101);
    refresh();
    assertThat(adapter.search("batch10-real-es-keyword", 1, 10).list())
        .noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo("batch10-es-adapter"));

    IndexResult reindex =
        adapter.reindex(
            List.of(
                new SearchDocument(
                    102,
                    "Batch 10 ES Reindex",
                    "batch10-es-reindex",
                    "summary batch10-real-es-reindex",
                    "body batch10-real-es-reindex",
                    "2026-05-18T00:00:00Z")));
    refresh();

    assertThat(reindex.indexed()).isEqualTo(1);
    assertThat(reindex.failed()).isZero();
    assertThat(adapter.search("batch10-real-es-reindex", 1, 10).list())
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo("batch10-es-reindex"));
  }

  private SearchProperties properties() {
    SearchProperties properties = new SearchProperties();
    properties.setStrategy(SearchStrategy.ELASTICSEARCH);
    properties.getElasticsearch().setUrl(baseUrl);
    properties.getElasticsearch().setIndexName(indexName);
    return properties;
  }

  private void refresh() throws Exception {
    request("POST", "/" + indexName + "/_refresh");
  }

  private void request(String method, String path) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(5))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isLessThan(500);
  }
}
