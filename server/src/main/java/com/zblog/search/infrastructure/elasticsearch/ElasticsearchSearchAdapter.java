package com.zblog.search.infrastructure.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.search.application.port.SearchIndexer;
import com.zblog.search.application.port.SearchPort;
import com.zblog.search.config.SearchProperties;
import com.zblog.search.domain.IndexResult;
import com.zblog.search.domain.SearchDocument;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchSearchAdapter implements SearchPort, SearchIndexer {

  // Elasticsearch adapter 同时承担查询和索引写入，外层通过 SearchService 统一降级和状态记录。
  private final SearchProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  public ElasticsearchSearchAdapter(SearchProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public PageResponse<Map<String, Object>> search(String keyword, int page, int pageSize) {
    int from = Math.max(0, page - 1) * pageSize;
    Map<String, Object> body =
        Map.of(
            "from",
            from,
            "size",
            pageSize,
            "query",
            query(keyword),
            "sort",
            List.of(Map.of("publish_time", Map.of("order", "desc")), Map.of("id", Map.of("order", "desc"))));
    Map<String, Object> response = request("POST", "/" + properties.getElasticsearch().getIndexName() + "/_search", body);
    Object hitsNode = response.get("hits");
    Map<?, ?> hits = hitsNode instanceof Map<?, ?> map ? map : Map.of();
    Object totalNode = hits.get("total");
    long total =
        totalNode instanceof Map<?, ?> totalMap && totalMap.get("value") instanceof Number number
            ? number.longValue()
            : 0;
    List<Map<String, Object>> rows =
        hitsList(hits)
            .stream()
                .map(hit -> (Map<?, ?>) hit)
                .map(this::sourceFromHit)
                .map(this::toArticleRow)
                .toList();
    return new PageResponse<>(rows, total, page, pageSize);
  }

  @Override
  public void upsert(SearchDocument document) {
    request("PUT", "/" + properties.getElasticsearch().getIndexName() + "/_doc/" + document.articleId(), source(document));
  }

  @Override
  public void delete(long articleId) {
    request("DELETE", "/" + properties.getElasticsearch().getIndexName() + "/_doc/" + articleId, null);
  }

  @Override
  public IndexResult reindex(List<SearchDocument> documents) {
    int indexed = 0;
    int failed = 0;
    for (SearchDocument document : documents) {
      try {
        upsert(document);
        indexed++;
      } catch (RuntimeException exception) {
        failed++;
      }
    }
    return new IndexResult(indexed, 0, failed);
  }

  private Map<String, Object> query(String keyword) {
    String normalized = keyword == null ? "" : keyword.trim();
    if (normalized.isBlank()) {
      // 空关键词沿用公开列表语义，交给 ES 按发布时间排序返回。
      return Map.of("match_all", Map.of());
    }
    return Map.of(
        "multi_match",
        Map.of(
            "query", normalized,
            "fields", List.of("title^3", "summary^2", "content_text")));
  }

  private Map<String, Object> source(SearchDocument document) {
    return Map.of(
        "id", document.articleId(),
        "title", document.title(),
        "slug", document.slug(),
        "url", "/posts/" + document.slug(),
        "summary", document.summary(),
        "content_text", document.contentText(),
        "publish_time", document.publishedAt() == null ? "" : document.publishedAt());
  }

  private Map<String, Object> toArticleRow(Map<String, Object> source) {
    Map<String, Object> row = new LinkedHashMap<>(source);
    row.putIfAbsent("id", source.get("article_id"));
    row.putIfAbsent("url", "/posts/" + source.get("slug"));
    row.putIfAbsent("publish_time", source.get("publish_time"));
    return row;
  }

  private List<?> hitsList(Map<?, ?> hits) {
    Object value = hits.get("hits");
    return value instanceof List<?> list ? list : List.of();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> sourceFromHit(Map<?, ?> hit) {
    Object source = hit.get("_source");
    return source instanceof Map<?, ?> ? (Map<String, Object>) source : Map.of();
  }

  private Map<String, Object> request(String method, String path, Map<String, Object> body) {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder()
              .uri(URI.create(properties.getElasticsearch().getUrl().replaceAll("/$", "") + path))
              .timeout(Duration.ofSeconds(5));
      if (body == null) {
        builder.method(method, HttpRequest.BodyPublishers.noBody());
      } else {
        builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        builder.header("Content-Type", "application/json");
      }
      HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      // delete 未命中视为幂等成功，其它 ES 错误交给上层 fallback/status 处理。
      if (response.statusCode() >= 400 && response.statusCode() != 404) {
        throw new IllegalStateException("Elasticsearch returned status " + response.statusCode());
      }
      if (response.body() == null || response.body().isBlank()) {
        return Map.of();
      }
      return objectMapper.readValue(response.body(), new TypeReference<>() {});
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Invalid Elasticsearch payload", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("Elasticsearch request failed", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Elasticsearch request interrupted", exception);
    }
  }
}
