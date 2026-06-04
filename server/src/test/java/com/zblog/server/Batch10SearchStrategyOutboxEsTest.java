package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties =
        "spring.datasource.url=jdbc:h2:mem:zblog_batch10;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
class Batch10SearchStrategyOutboxEsTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void dbSearchIsDefaultAndSearchStatusIsExplicit() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId = createArticle(headers, "batch10-db-default", "Batch 10 DB Default", "batch10-db-default-keyword", true);
    long draftId = createArticle(headers, "batch10-db-draft", "Batch 10 DB Draft", "batch10-db-draft-keyword", false);

    assertSearchContains("batch10-db-default-keyword", "batch10-db-default");
    assertSearchDoesNotContain("batch10-db-draft-keyword", "batch10-db-draft");

    ResponseEntity<Map> status =
        restTemplate.exchange("/api/v1/admin/search/status", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> data = data(status);
    assertThat(data.get("strategy")).isEqualTo("db");
    assertThat(data.get("elasticsearch_enabled")).isEqualTo(false);
    assertThat(data.get("fallback_to_db")).isEqualTo(true);

    deleteArticle(headers, articleId);
    deleteArticle(headers, draftId);
  }

  @Test
  void articleLifecycleCreatesSearchOutboxEventsAndConsumerIsIdempotent() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId =
        createArticle(
            headers,
            "batch10-search-events",
            "Batch 10 Search Events",
            "batch10-search-events-initial",
            true);

    assertThat(outboxCount("ARTICLE_SEARCH_UPSERT", articleId)).isEqualTo(1L);

    ResponseEntity<Map> firstDrain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    assertThat(firstDrain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(searchConsumptionCount(articleId)).isEqualTo(1L);

    ResponseEntity<Map> secondDrain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    assertThat(secondDrain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(searchConsumptionCount(articleId)).isEqualTo(1L);

    updateArticle(headers, articleId, "Batch 10 Search Events Updated", "batch10-search-events-updated");
    assertThat(outboxCount("ARTICLE_SEARCH_UPSERT", articleId)).isEqualTo(2L);

    restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId + "/unpublish",
        HttpMethod.POST,
        new HttpEntity<>(headers),
        Map.class);
    assertThat(outboxCount("ARTICLE_SEARCH_DELETE", articleId)).isEqualTo(1L);

    deleteArticle(headers, articleId);
  }

  @Test
  void reindexOnlyIndexesPublishedArticlesAndRecordsStatus() {
    HttpHeaders headers = authenticatedHeaders();
    long publishedId =
        createArticle(headers, "batch10-reindex-published", "Batch 10 Reindex Published", "batch10-reindex-published-keyword", true);
    long draftId = createArticle(headers, "batch10-reindex-draft", "Batch 10 Reindex Draft", "batch10-reindex-draft-keyword", false);

    ResponseEntity<Map> reindex =
        restTemplate.exchange("/api/v1/admin/search/reindex", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    assertThat(reindex.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> data = data(reindex);
    assertThat(number(data, "indexed")).isGreaterThanOrEqualTo(1);
    assertThat(number(data, "failed")).isEqualTo(0);

    Map<?, ?> status =
        data(restTemplate.exchange("/api/v1/admin/search/status", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
    assertThat(number(status, "last_reindex_indexed")).isGreaterThanOrEqualTo(1);
    assertThat(status.get("last_error")).isNull();

    assertSearchContains("batch10-reindex-published-keyword", "batch10-reindex-published");
    assertSearchDoesNotContain("batch10-reindex-draft-keyword", "batch10-reindex-draft");

    deleteArticle(headers, publishedId);
    deleteArticle(headers, draftId);
  }

  @Test
  void indexingFailureIsVisibleAndRetryable() {
    HttpHeaders headers = authenticatedHeaders();
    long eventId =
        insertOutboxEvent("ARTICLE_SEARCH_DELETE", 987654321L, "not-json");

    ResponseEntity<Map> failedDrain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    assertThat(failedDrain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(jdbcTemplate.queryForObject("select status from event_outbox where id = ?", String.class, eventId))
        .isEqualTo("failed");

    Map<?, ?> failedStatus =
        data(restTemplate.exchange("/api/v1/admin/search/status", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
    assertThat(failedStatus.get("last_error").toString()).contains("Invalid search event payload");

    jdbcTemplate.update(
        "update event_outbox set payload = ?, attempts = 0 where id = ?",
        "{\"article_id\":987654321}",
        eventId);
    ResponseEntity<Map> retryDrain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    assertThat(retryDrain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(jdbcTemplate.queryForObject("select status from event_outbox where id = ?", String.class, eventId))
        .isEqualTo("sent");
  }

  private long createArticle(HttpHeaders headers, String slug, String title, String keyword, boolean publish) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title", title,
                    "slug", slug,
                    "summary", "summary " + keyword,
                    "content", "# " + title + "\n\nbody " + keyword,
                    "is_publish", publish),
                headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return number(data(response), "id");
  }

  private void updateArticle(HttpHeaders headers, long articleId, String title, String keyword) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId,
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "title", title,
                    "summary", "summary " + keyword,
                    "content", "# " + title + "\n\nbody " + keyword),
                headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private void deleteArticle(HttpHeaders headers, long articleId) {
    restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        Map.class);
  }

  private long insertOutboxEvent(String eventType, long aggregateId, String payload) {
    jdbcTemplate.update(
        """
        insert into event_outbox (event_type, aggregate_type, aggregate_id, payload, status)
        values (?, 'article', ?, ?, 'pending')
        """,
        eventType,
        aggregateId,
        payload);
    return jdbcTemplate.queryForObject(
        "select id from event_outbox where event_type = ? and aggregate_id = ? order by id desc limit 1",
        Long.class,
        eventType,
        aggregateId);
  }

  private long outboxCount(String eventType, long articleId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from event_outbox where event_type = ? and aggregate_id = ?",
        Long.class,
        eventType,
        articleId);
  }

  private long searchConsumptionCount(long articleId) {
    return jdbcTemplate.queryForObject(
        """
        select count(*)
        from event_consumptions c
        join event_outbox o on o.id = c.event_id
        where c.consumer_name = 'article-search-index' and o.aggregate_id = ?
        """,
        Long.class,
        articleId);
  }

  private void assertSearchContains(String keyword, String slug) {
    List<?> list = search(keyword);
    assertThat(list)
        .as("search results for %s should contain %s", keyword, slug)
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo(slug));
  }

  private void assertSearchDoesNotContain(String keyword, String slug) {
    List<?> list = search(keyword);
    assertThat(list)
        .as("search results for %s should not contain %s", keyword, slug)
        .noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo(slug));
  }

  private List<?> search(String keyword) {
    ResponseEntity<Map> response =
        restTemplate.getForEntity("/api/v1/articles/search?keyword=" + keyword, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (List<?>) data(response).get("list");
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(data(response).get("access_token").toString());
    return headers;
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    return data(response.getBody());
  }

  private Map<?, ?> data(Map<?, ?> body) {
    Object data = body.get("data");
    assertThat(data).isInstanceOf(Map.class);
    return (Map<?, ?>) data;
  }

  private long number(Map<?, ?> map, String key) {
    Object value = map.get(key);
    assertThat(value).isInstanceOf(Number.class);
    return ((Number) value).longValue();
  }
}
