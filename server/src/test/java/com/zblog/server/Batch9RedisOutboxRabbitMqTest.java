package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.PreparedStatement;
import java.sql.Statement;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "zblog.cache.article-view-dedup-seconds=1")
class Batch9RedisOutboxRabbitMqTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void articlePageviewIsDedupedAndFeedsHotRanking() throws InterruptedException {
    long articleId = insertArticle("batch9-hot-article", "Batch 9 Hot Article", true, 7);
    long beforeViews = viewCount(articleId);

    ResponseEntity<Map> first = collect(articleId, "/posts/batch9-hot-article", "batch9-screen");
    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> firstData = data(first);
    assertThat(firstData.get("article_view_counted")).isEqualTo(true);
    assertThat(number(firstData, "article_view_count")).isEqualTo(beforeViews + 1);
    assertThat(viewCount(articleId)).isEqualTo(beforeViews + 1);

    ResponseEntity<Map> duplicate = collect(articleId, "/posts/batch9-hot-article", "batch9-screen");
    assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> duplicateData = data(duplicate);
    assertThat(duplicateData.get("article_view_counted")).isEqualTo(false);
    assertThat(number(duplicateData, "article_view_count")).isEqualTo(beforeViews + 1);
    assertThat(viewCount(articleId)).isEqualTo(beforeViews + 1);

    Thread.sleep(1500);
    ResponseEntity<Map> afterDedupWindow = collect(articleId, "/posts/batch9-hot-article", "batch9-screen");
    assertThat(afterDedupWindow.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(afterDedupWindow).get("article_view_counted")).isEqualTo(true);
    assertThat(number(data(afterDedupWindow), "article_view_count")).isEqualTo(beforeViews + 2);
    assertThat(viewCount(articleId)).isEqualTo(beforeViews + 2);

    Map<?, ?> hotPage = data(restTemplate.getForEntity("/api/v1/articles/hot?limit=5", Map.class));
    List<?> hotArticles = (List<?>) hotPage.get("list");
    assertThat(hotArticles)
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo("batch9-hot-article"));
  }

  @Test
  void hotArticlesExposeRecentRedisRankingAndTotalViewRankingSeparately() {
    insertArticle("batch9-hot-high", "Batch 9 Hot High", true, 100);
    long lowId = insertArticle("batch9-hot-low", "Batch 9 Hot Low", true, 1);
    collect(lowId, "/posts/batch9-hot-low", "hot-order-screen");

    Map<?, ?> recentPage = data(restTemplate.getForEntity("/api/v1/articles/hot?type=recent&limit=5", Map.class));
    List<?> recentArticles = (List<?>) recentPage.get("list");
    assertThat(recentArticles).isNotEmpty();
    assertThat(((Map<?, ?>) recentArticles.getFirst()).get("slug")).isEqualTo("batch9-hot-low");
    assertThat(((Map<?, ?>) recentArticles.getFirst()).get("hot_score")).isInstanceOf(Number.class);

    Map<?, ?> totalPage = data(restTemplate.getForEntity("/api/v1/articles/hot?type=total&limit=5", Map.class));
    List<?> totalArticles = (List<?>) totalPage.get("list");
    assertThat(totalArticles).isNotEmpty();
    assertThat(((Map<?, ?>) totalArticles.getFirst()).get("slug")).isEqualTo("batch9-hot-high");
    assertThat(
            totalArticles.stream()
                .map(row -> ((Number) ((Map<?, ?>) row).get("view_count")).longValue())
                .toList())
        .isSortedAccordingTo(java.util.Comparator.reverseOrder());
  }

  @Test
  void collectRateLimitReturnsTooManyRequests() {
    long articleId = insertArticle("batch9-rate-limit", "Batch 9 Rate Limit", true, 0);
    ResponseEntity<Map> last = null;
    for (int i = 0; i < 130; i++) {
      last = collect(articleId, "/posts/batch9-rate-limit?i=" + i, "rate-limit-screen");
      if (last.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
        break;
      }
    }
    assertThat(last).isNotNull();
    assertThat(last.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void siteStatsCanBeCachedAndInvalidatedWithoutChangingContract() {
    Map<?, ?> first = data(restTemplate.getForEntity("/api/v1/stats/site", Map.class));
    Map<?, ?> second = data(restTemplate.getForEntity("/api/v1/stats/site", Map.class));
    assertThat(second.get("total_page_views")).isEqualTo(first.get("total_page_views"));
    assertThat(second.keySet().stream().map(Object::toString).toList())
        .contains("total_articles", "total_comments", "total_page_views", "today_pageviews");
  }

  @Test
  void articlePublishCreatesOutboxEventAndRabbitConsumerCreatesNotificationIdempotently() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId = insertArticle("batch9-publish-event", "Batch 9 Publish Event", false, 0);

    ResponseEntity<Map> publish =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/publish",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(publish.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<String, Object> outbox =
        jdbcTemplate.queryForMap(
            "select * from event_outbox where event_type = 'ARTICLE_PUBLISHED' and aggregate_id = ?",
            articleId);
    assertThat(outbox.get("status")).isEqualTo("pending");

    ResponseEntity<Map> drain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(drain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(number(data(drain), "published")).isGreaterThanOrEqualTo(1);

    assertThat(
            jdbcTemplate.queryForObject(
                "select status from event_outbox where id = ?", String.class, outbox.get("id")))
        .isEqualTo("sent");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from notifications where type = 'article_published' and target_id = ?",
                Long.class,
                articleId))
        .isEqualTo(1L);
    assertThat(articlePublishedNotificationLink(articleId)).isEqualTo("/articles/edit/" + articleId);

    ResponseEntity<Map> secondDrain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(secondDrain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from notifications where type = 'article_published' and target_id = ?",
                Long.class,
                articleId))
        .isEqualTo(1L);
    assertThat(articlePublishedNotificationLink(articleId)).isEqualTo("/articles/edit/" + articleId);
  }

  @Test
  void directCreateAsPublishedAlsoCreatesOutboxNotificationEvent() {
    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> create =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title", "Batch 9 Direct Publish",
                    "slug", "batch9-direct-publish",
                    "content", "direct create publish should emit outbox event",
                    "is_publish", true),
                headers),
            Map.class);
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.OK);
    long articleId = number(data(create), "id");

    Long outboxCount =
        jdbcTemplate.queryForObject(
            "select count(*) from event_outbox where event_type = 'ARTICLE_PUBLISHED' and aggregate_id = ?",
            Long.class,
            articleId);
    assertThat(outboxCount).isEqualTo(1L);

    ResponseEntity<Map> drain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(drain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from notifications where type = 'article_published' and target_id = ?",
                Long.class,
                articleId))
        .isEqualTo(1L);
  }

  @Test
  void duplicateArticleSlugReturnsBusinessErrorInsteadOfInternalServerError() {
    HttpHeaders headers = authenticatedHeaders();
    insertArticle("batch9-duplicate-slug", "Batch 9 Duplicate Slug", false, 0);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title", "Batch 9 Duplicate Slug Again",
                    "slug", "batch9-duplicate-slug",
                    "content", "duplicate slug should be a business error"),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("message").toString()).contains("slug");
  }

  private ResponseEntity<Map> collect(long articleId, String url, String screen) {
    return restTemplate.postForEntity(
        "/api/v1/collect",
        Map.of(
            "type",
            "pageview",
            "url",
            url,
            "hostname",
            "localhost",
            "title",
            "Batch 9",
            "screen",
            screen,
            "language",
            "zh-CN",
            "article_id",
            articleId,
            "timestamp",
            System.currentTimeMillis()),
        Map.class);
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

  private long insertArticle(String slug, String title, boolean published, long viewCount) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  insert into articles (
                    title, slug, content_markdown, content_html, content_text, summary,
                    status, view_count, published_at
                  ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
                  """,
                  Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, title);
          statement.setString(2, slug);
          statement.setString(3, title + " markdown");
          statement.setString(4, "<p>" + title + "</p>");
          statement.setString(5, title + " text");
          statement.setString(6, title + " summary");
          statement.setString(7, published ? "PUBLISHED" : "DRAFT");
          statement.setLong(8, viewCount);
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }

  private long viewCount(long articleId) {
    return jdbcTemplate.queryForObject("select view_count from articles where id = ?", Long.class, articleId);
  }

  private String articlePublishedNotificationLink(long articleId) {
    return jdbcTemplate.queryForObject(
        "select link from notifications where type = 'article_published' and target_id = ? order by id desc limit 1",
        String.class,
        articleId);
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
