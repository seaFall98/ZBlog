package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch6SearchSeoDepthTest {

  private static final String SLUG = "batch-6-search-seo-depth";
  private static final String DRAFT_SLUG = "batch-6-search-seo-draft";
  private static final String INITIAL_KEYWORD = "batch6-initial-keyword";
  private static final String UPDATED_KEYWORD = "batch6-updated-keyword";
  private static final String DRAFT_KEYWORD = "batch6-draft-keyword";

  @org.springframework.beans.factory.annotation.Autowired private TestRestTemplate restTemplate;

  @AfterEach
  void cleanup() {
    HttpHeaders headers = authenticatedHeaders();
    deleteBySlug(SLUG, headers);
    deleteBySlug(DRAFT_SLUG, headers);
  }

  @Test
  void adminCreateWithPublishFlagImmediatelyEntersSearchAndSeoOutputs() {
    HttpHeaders headers = authenticatedHeaders();

    Number articleId =
        articleId(
            (Map<?, ?>)
                data(
                    restTemplate.exchange(
                        "/api/v1/admin/articles",
                        HttpMethod.POST,
                        new HttpEntity<>(
                            Map.of(
                                "title", "Batch 6 Direct Publish " + INITIAL_KEYWORD,
                                "slug", SLUG,
                                "summary", "Direct publish summary " + INITIAL_KEYWORD,
                                "content", "# Batch 6 Direct Publish\n\nDirect publish body " + INITIAL_KEYWORD,
                                "is_publish", true),
                            headers),
                        Map.class)));

    assertSearchContains(INITIAL_KEYWORD, SLUG, "Batch 6 Direct Publish " + INITIAL_KEYWORD);
    assertSeoContains(SLUG, "Batch 6 Direct Publish " + INITIAL_KEYWORD, "Direct publish summary " + INITIAL_KEYWORD);

    restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        Map.class);
  }

  @Test
  void searchAndSeoOutputsTrackArticleLifecycleAndPublishedState() {
    HttpHeaders headers = authenticatedHeaders();

    Number draftId =
        articleId(
            (Map<?, ?>)
                data(
                    restTemplate.exchange(
                        "/api/v1/admin/articles",
                        HttpMethod.POST,
                        new HttpEntity<>(
                            Map.of(
                                "title", "Batch 6 Draft Title",
                                "slug", DRAFT_SLUG,
                                "summary", "Draft summary " + DRAFT_KEYWORD,
                                "content", "Draft body " + DRAFT_KEYWORD),
                            headers),
                        Map.class)));
    assertSearchDoesNotContain(DRAFT_KEYWORD, DRAFT_SLUG);
    assertSeoDoesNotContain(DRAFT_SLUG, DRAFT_KEYWORD);

    Number articleId =
        articleId(
            (Map<?, ?>)
                data(
                    restTemplate.exchange(
                        "/api/v1/admin/articles",
                        HttpMethod.POST,
                        new HttpEntity<>(
                            Map.of(
                                "title", "Batch 6 Initial Title " + INITIAL_KEYWORD,
                                "slug", SLUG,
                                "summary", "Initial summary " + INITIAL_KEYWORD,
                                "content", "# Batch 6\n\nInitial body " + INITIAL_KEYWORD),
                            headers),
                        Map.class)));
    assertSearchDoesNotContain(INITIAL_KEYWORD, SLUG);
    assertSeoDoesNotContain(SLUG, INITIAL_KEYWORD);

    data(
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/publish",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class));
    assertSearchContains(INITIAL_KEYWORD, SLUG, "Batch 6 Initial Title " + INITIAL_KEYWORD);
    assertSeoContains(SLUG, "Batch 6 Initial Title " + INITIAL_KEYWORD, "Initial summary " + INITIAL_KEYWORD);
    assertSeoDoesNotContain(DRAFT_SLUG, DRAFT_KEYWORD);

    data(
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId,
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "title", "Batch 6 Updated Title " + UPDATED_KEYWORD,
                    "summary", "Updated summary " + UPDATED_KEYWORD,
                    "content", "# Batch 6 Updated\n\nUpdated body " + UPDATED_KEYWORD),
                headers),
            Map.class));
    assertSearchDoesNotContain(INITIAL_KEYWORD, SLUG);
    assertSearchContains(UPDATED_KEYWORD, SLUG, "Batch 6 Updated Title " + UPDATED_KEYWORD);
    assertSeoDoesNotContainKeyword(INITIAL_KEYWORD);
    assertSeoContains(SLUG, "Batch 6 Updated Title " + UPDATED_KEYWORD, "Updated summary " + UPDATED_KEYWORD);

    data(
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/unpublish",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class));
    assertSearchDoesNotContain(UPDATED_KEYWORD, SLUG);
    assertSeoDoesNotContain(SLUG, UPDATED_KEYWORD);

    data(
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/publish",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class));
    assertSearchContains(UPDATED_KEYWORD, SLUG, "Batch 6 Updated Title " + UPDATED_KEYWORD);
    assertSeoContains(SLUG, "Batch 6 Updated Title " + UPDATED_KEYWORD, "Updated summary " + UPDATED_KEYWORD);

    restTemplate.exchange(
        "/api/v1/admin/articles/" + articleId,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        Map.class);
    assertSearchDoesNotContain(UPDATED_KEYWORD, SLUG);
    assertSeoDoesNotContain(SLUG, UPDATED_KEYWORD);

    restTemplate.exchange(
        "/api/v1/admin/articles/" + draftId,
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        Map.class);
    assertSearchDoesNotContain(DRAFT_KEYWORD, DRAFT_SLUG);
    assertSeoDoesNotContain(DRAFT_SLUG, DRAFT_KEYWORD);
  }

  private void assertSearchContains(String keyword, String slug, String title) {
    Map<?, ?> page = search(keyword);
    List<?> list = (List<?>) page.get("list");
    assertThat(list)
        .as("search results for %s should contain %s", keyword, slug)
        .anySatisfy(
            row -> {
              Map<?, ?> article = (Map<?, ?>) row;
              assertThat(article.get("slug")).isEqualTo(slug);
              assertThat(article.get("title")).isEqualTo(title);
            });
  }

  private void assertSearchDoesNotContain(String keyword, String slug) {
    Map<?, ?> page = search(keyword);
    List<?> list = (List<?>) page.get("list");
    assertThat(list)
        .as("search results for %s should not contain %s", keyword, slug)
        .noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("slug")).isEqualTo(slug));
  }

  private Map<?, ?> search(String keyword) {
    ResponseEntity<Map> response =
        restTemplate.getForEntity("/api/v1/articles/search?keyword=" + keyword, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (Map<?, ?>) data(response);
  }

  private void assertSeoContains(String slug, String title, String summary) {
    String rss = xml("/rss.xml");
    String atom = xml("/atom.xml");
    String sitemap = xml("/sitemap.xml");
    assertThat(rss).contains("/posts/" + slug, title, summary);
    assertThat(atom).contains("/posts/" + slug, title, summary);
    assertThat(sitemap).contains("/posts/" + slug);
  }

  private void assertSeoDoesNotContain(String slug, String keyword) {
    String rss = xml("/rss.xml");
    String atom = xml("/atom.xml");
    String sitemap = xml("/sitemap.xml");
    assertThat(rss).doesNotContain("/posts/" + slug, keyword);
    assertThat(atom).doesNotContain("/posts/" + slug, keyword);
    assertThat(sitemap).doesNotContain("/posts/" + slug, keyword);
  }

  private void assertSeoDoesNotContainKeyword(String keyword) {
    assertThat(xml("/rss.xml")).doesNotContain(keyword);
    assertThat(xml("/atom.xml")).doesNotContain(keyword);
    assertThat(xml("/sitemap.xml")).doesNotContain(keyword);
  }

  private String xml(String path) {
    ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotBlank();
    return response.getBody();
  }

  private Number articleId(Map<?, ?> article) {
    return (Number) article.get("id");
  }

  private void deleteBySlug(String slug, HttpHeaders headers) {
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles?keyword=" + slug + "&page_size=20",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    if (!HttpStatus.OK.equals(listResponse.getStatusCode())) {
      return;
    }
    Map<?, ?> page = (Map<?, ?>) data(listResponse);
    for (Object row : (List<?>) page.get("list")) {
      Map<?, ?> article = (Map<?, ?>) row;
      if (slug.equals(article.get("slug"))) {
        restTemplate.exchange(
            "/api/v1/admin/articles/" + article.get("id"),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);
      }
    }
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
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }
}
