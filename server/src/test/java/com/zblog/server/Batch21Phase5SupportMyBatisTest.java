package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.search.application.port.SearchStatusRepository;
import com.zblog.seo.application.port.SeoFeedRepository;
import com.zblog.stats.application.port.VisitRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch21Phase5SupportMyBatisTest {

  @Autowired private VisitRepository visitRepository;

  @Autowired private SeoFeedRepository seoFeedRepository;

  @Autowired private SearchStatusRepository searchStatusRepository;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void supportRepositoriesUseMyBatisAdapters() {
    assertThat(AopUtils.getTargetClass(visitRepository).getName())
        .isEqualTo("com.zblog.stats.infrastructure.mybatis.MyBatisVisitRepository");
    assertThat(AopUtils.getTargetClass(seoFeedRepository).getName())
        .isEqualTo("com.zblog.seo.infrastructure.mybatis.MyBatisSeoFeedRepository");
    assertThat(AopUtils.getTargetClass(searchStatusRepository).getName())
        .isEqualTo("com.zblog.search.infrastructure.mybatis.MyBatisSearchStatusRepository");
  }

  @Test
  void seoFeedsThroughMyBatisRepositoryIncludePublishedAndExcludeDraftArticles() {
    insertArticle("batch21-phase5-seo-published", "Batch21 Phase5 SEO Published", true);
    insertArticle("batch21-phase5-seo-draft", "Batch21 Phase5 SEO Draft", false);

    ResponseEntity<String> rssResponse = restTemplate.getForEntity("/rss.xml", String.class);
    ResponseEntity<String> sitemapResponse = restTemplate.getForEntity("/sitemap.xml", String.class);

    assertThat(rssResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(rssResponse.getBody())
        .contains("Batch21 Phase5 SEO Published")
        .doesNotContain("Batch21 Phase5 SEO Draft");
    assertThat(sitemapResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(sitemapResponse.getBody())
        .contains("/posts/batch21-phase5-seo-published")
        .doesNotContain("/posts/batch21-phase5-seo-draft");
  }

  @Test
  void searchStatusThroughMyBatisRepositoryKeepsDefaultsReindexAndErrorFields() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> initialStatus = adminSearchStatus(headers);
    assertThat(initialStatus.get("strategy")).isEqualTo("db");
    assertThat(initialStatus.get("elasticsearch_enabled")).isEqualTo(false);
    assertThat(initialStatus.get("fallback_to_db")).isEqualTo(true);

    ResponseEntity<Map> reindexResponse =
        restTemplate.exchange(
            "/api/v1/admin/search/reindex", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    assertThat(reindexResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> afterReindex = adminSearchStatus(headers);
    assertThat(number(afterReindex, "last_reindex_indexed")).isGreaterThanOrEqualTo(1);
    assertThat(number(afterReindex, "last_reindex_failed")).isEqualTo(0);
    assertThat(afterReindex.get("last_error")).isNull();

    long eventId = insertOutboxEvent("ARTICLE_SEARCH_DELETE", 987654329L, "not-json");
    ResponseEntity<Map> failedDrain =
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    assertThat(failedDrain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(jdbcTemplate.queryForObject("select status from event_outbox where id = ?", String.class, eventId))
        .isEqualTo("failed");
    assertThat(adminSearchStatus(headers).get("last_error").toString())
        .contains("Invalid search event payload");
  }

  @Test
  void collectPageviewThroughMyBatisVisitRepositoryKeepsArticleAndSiteStats() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId = insertArticle("batch21-phase5-visit", "Batch21 Phase5 Visit", true);
    long beforeViews = articleViewCount(articleId);
    long beforeSitePv = number(data(restTemplate.getForEntity("/api/v1/stats/site", Map.class)), "total_page_views");

    ResponseEntity<Map> collectResponse =
        restTemplate.postForEntity(
            "/api/v1/collect",
            Map.of(
                "type", "pageview",
                "url", "/posts/batch21-phase5-visit",
                "hostname", "localhost",
                "title", "Batch21 Phase5 Visit",
                "screen", "1440x900",
                "language", "zh-CN",
                "article_id", articleId,
                "timestamp", System.currentTimeMillis()),
            Map.class);

    assertThat(collectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(articleViewCount(articleId)).isEqualTo(beforeViews + 1);
    long afterSitePv = number(data(restTemplate.getForEntity("/api/v1/stats/site", Map.class)), "total_page_views");
    assertThat(afterSitePv).isEqualTo(beforeSitePv + 1);

    Map<?, ?> adminVisits =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/stats/visits?keyword=batch21-phase5-visit",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class));
    assertThat((List<?>) adminVisits.get("list"))
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("url")).isEqualTo("/posts/batch21-phase5-visit"));
  }

  private long insertArticle(String slug, String title, boolean published) {
    jdbcTemplate.update(
        """
        insert into articles (
          slug, title, summary, content_markdown, content_html, content_text, status, published_at
        ) values (?, ?, ?, ?, ?, ?, ?, current_timestamp)
        """,
        slug,
        title,
        "summary " + slug,
        "# " + title,
        "<h1>" + title + "</h1>",
        title + " body",
        published ? "PUBLISHED" : "DRAFT");
    return jdbcTemplate.queryForObject("select id from articles where slug = ?", Long.class, slug);
  }

  private long articleViewCount(long articleId) {
    return jdbcTemplate.queryForObject("select view_count from articles where id = ?", Long.class, articleId);
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

  private Map<?, ?> adminSearchStatus(HttpHeaders headers) {
    return (Map<?, ?>)
        data(
            restTemplate.exchange(
                "/api/v1/admin/search/status", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(((Map<?, ?>) data(response)).get("access_token").toString());
    return headers;
  }

  private Object data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    return response.getBody().get("data");
  }

  private long number(Object object, String key) {
    Object value = ((Map<?, ?>) object).get(key);
    assertThat(value).isInstanceOf(Number.class);
    return ((Number) value).longValue();
  }
}
