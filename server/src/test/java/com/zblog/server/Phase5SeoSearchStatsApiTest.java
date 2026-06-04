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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Phase5SeoSearchStatsApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void publicSearchReturnsPublishedArticles() {
    ResponseEntity<Map> response =
        restTemplate.getForEntity("/api/v1/articles/search?keyword=ZBlog", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> page = (Map<?, ?>) data(response);
    assertThat((List<?>) page.get("list")).isNotEmpty();
    assertThat(((Number) page.get("total")).longValue()).isGreaterThanOrEqualTo(1);
  }

  @Test
  void publicStatsExposeSiteAndArchiveData() {
    ResponseEntity<Map> siteResponse = restTemplate.getForEntity("/api/v1/stats/site", Map.class);
    ResponseEntity<Map> archiveResponse =
        restTemplate.getForEntity("/api/v1/stats/archives", Map.class);

    assertThat(siteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> site = (Map<?, ?>) data(siteResponse);
    assertThat(((Number) site.get("total_articles")).longValue()).isGreaterThanOrEqualTo(1);
    assertThat(site.containsKey("total_words")).isTrue();
    assertThat(site.containsKey("total_comments")).isTrue();
    assertThat(site.containsKey("total_friends")).isTrue();

    assertThat(archiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> archives = (Map<?, ?>) data(archiveResponse);
    assertThat((List<?>) archives.get("archives")).isNotEmpty();
  }

  @Test
  void seoFeedsAreServedAsXml() {
    ResponseEntity<String> rssResponse = restTemplate.getForEntity("/rss.xml", String.class);
    ResponseEntity<String> atomResponse = restTemplate.getForEntity("/atom.xml", String.class);
    ResponseEntity<String> sitemapResponse = restTemplate.getForEntity("/sitemap.xml", String.class);

    assertThat(rssResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(rssResponse.getBody()).contains("<rss", "Hello ZBlog");

    assertThat(atomResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(atomResponse.getBody()).contains("<feed", "Hello ZBlog");

    assertThat(sitemapResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(sitemapResponse.getBody()).contains("<urlset", "/posts/hello-zblog");
  }

  @Test
  void adminStatsExposeDashboardAndBreakdownData() {
    insertDraftArticleWithPublishedTag();
    HttpHeaders headers = authenticatedHeaders();

    ResponseEntity<Map> dashboard =
        restTemplate.exchange(
            "/api/v1/admin/stats/dashboard", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    ResponseEntity<Map> category =
        restTemplate.exchange(
            "/api/v1/admin/stats/category", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    ResponseEntity<Map> tag =
        restTemplate.exchange(
            "/api/v1/admin/stats/tag", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    ResponseEntity<Map> trend =
        restTemplate.exchange(
            "/api/v1/admin/stats/trend?start_date=2026-01-01&end_date=2026-01-07&type=daily",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Number) data(dashboard, "total_articles")).longValue()).isGreaterThanOrEqualTo(1);
    assertThat(category.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) data(category)).isNotEmpty();
    assertThat(tag.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<?> tags = (List<?>) data(tag);
    assertThat(tags).isNotEmpty();
    assertThat(countForName(tags, "ZBlog")).isEqualTo(1);
    assertThat(trend.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) data(trend)).hasSize(7);
  }

  private void insertDraftArticleWithPublishedTag() {
    jdbcTemplate.update(
        """
        insert into articles (
          slug, title, summary, content_markdown, content_html, content_text, status, category_id
        )
        select 'draft-zblog-for-stats', 'Draft ZBlog', 'Draft summary',
          'Draft markdown', '<p>Draft</p>', 'Draft text', 'DRAFT', c.id
        from categories c
        where c.slug = 'default'
          and not exists (select 1 from articles where slug = 'draft-zblog-for-stats')
        """);
    jdbcTemplate.update(
        """
        insert into article_tags (article_id, tag_id)
        select a.id, t.id
        from articles a, tags t
        where a.slug = 'draft-zblog-for-stats'
          and t.slug = 'zblog'
          and not exists (
            select 1 from article_tags at where at.article_id = a.id and at.tag_id = t.id
          )
        """);
  }

  private long countForName(List<?> rows, String name) {
    return rows.stream()
        .map(row -> (Map<?, ?>) row)
        .filter(row -> name.equals(row.get("name")))
        .map(row -> ((Number) row.get("count")).longValue())
        .findFirst()
        .orElse(0L);
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);
    String token = ((Map<?, ?>) data(response)).get("access_token").toString();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Object data(ResponseEntity<Map> response) {
    return response.getBody().get("data");
  }

  private Object data(ResponseEntity<Map> response, String key) {
    return ((Map<?, ?>) data(response)).get(key);
  }
}
