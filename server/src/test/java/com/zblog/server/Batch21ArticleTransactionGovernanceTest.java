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
class Batch21ArticleTransactionGovernanceTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void createRollsBackArticleWhenTagReplacementFails() {
    HttpHeaders headers = authenticatedHeaders();
    String slug = "batch21-create-rollback";

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title",
                    "Batch21 Create Rollback",
                    "slug",
                    slug,
                    "summary",
                    "Batch21 create rollback summary",
                    "content",
                    "# Batch21\n\ncreate rollback",
                    "tag_ids",
                    List.of(987654321L)),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(articleCountBySlug(slug)).isZero();
  }

  @Test
  void updateRollsBackArticleAndTagsWhenTagReplacementFails() {
    HttpHeaders headers = authenticatedHeaders();
    long tagId = createTag(headers, "Batch21 Stable Tag", "batch21-stable-tag");
    String slug = "batch21-update-rollback";
    long articleId = createArticle(headers, slug, "Batch21 Stable Title", tagId);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId,
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "title",
                    "Batch21 Mutated Title",
                    "summary",
                    "Batch21 mutated summary",
                    "content",
                    "# Batch21\n\nmutated content",
                    "tag_ids",
                    List.of(987654322L)),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    Map<String, Object> article = articleById(articleId);
    assertThat(article.get("title")).isEqualTo("Batch21 Stable Title");
    assertThat(article.get("summary")).isEqualTo("Batch21 stable summary");
    assertThat(article.get("content_markdown")).isEqualTo("# Batch21\n\nstable content");
    assertThat(tagIds(articleId)).containsExactly(tagId);

    deleteArticle(headers, articleId);
  }

  private long createTag(HttpHeaders headers, String name, String slug) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/tags",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", name, "slug", slug, "description", "Batch21"), headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return number(data(response), "id");
  }

  private long createArticle(HttpHeaders headers, String slug, String title, long tagId) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title",
                    title,
                    "slug",
                    slug,
                    "summary",
                    "Batch21 stable summary",
                    "content",
                    "# Batch21\n\nstable content",
                    "tag_ids",
                    List.of(tagId)),
                headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return number(data(response), "id");
  }

  private void deleteArticle(HttpHeaders headers, long articleId) {
    restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
  }

  private long articleCountBySlug(String slug) {
    return jdbcTemplate.queryForObject(
        "select count(*) from articles where slug = ?", Long.class, slug);
  }

  private Map<String, Object> articleById(long articleId) {
    return jdbcTemplate.queryForMap(
        "select title, summary, content_markdown from articles where id = ?", articleId);
  }

  private List<Long> tagIds(long articleId) {
    return jdbcTemplate.queryForList(
        "select tag_id from article_tags where article_id = ? order by tag_id", Long.class, articleId);
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
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

  private long number(Map<?, ?> data, String key) {
    return ((Number) data.get(key)).longValue();
  }
}
