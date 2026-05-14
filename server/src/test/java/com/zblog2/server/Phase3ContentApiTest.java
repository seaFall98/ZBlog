package com.zblog2.server;

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
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Phase3ContentApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void publicApisExposeSeededBlogContentForFrontend() {
    ResponseEntity<Map> menusResponse = restTemplate.getForEntity("/api/v1/menus", Map.class);
    ResponseEntity<Map> categoriesResponse =
        restTemplate.getForEntity("/api/v1/categories", Map.class);
    ResponseEntity<Map> tagsResponse = restTemplate.getForEntity("/api/v1/tags", Map.class);
    ResponseEntity<Map> articlesResponse = restTemplate.getForEntity("/api/v1/articles", Map.class);
    ResponseEntity<Map> articleResponse =
        restTemplate.getForEntity("/api/v1/articles/hello-zblog2", Map.class);
    ResponseEntity<Map> randomResponse =
        restTemplate.getForEntity("/api/v1/articles/random", Map.class);

    assertThat(menusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) data(menusResponse)).isNotEmpty();

    assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) ((Map<?, ?>) data(categoriesResponse)).get("list")).isNotEmpty();

    assertThat(tagsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) ((Map<?, ?>) data(tagsResponse)).get("list")).isNotEmpty();

    assertThat(articlesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> articlePage = (Map<?, ?>) data(articlesResponse);
    assertThat((List<?>) articlePage.get("list")).isNotEmpty();
    assertThat(((Number) articlePage.get("total")).longValue()).isGreaterThanOrEqualTo(1);

    assertThat(articleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> article = (Map<?, ?>) data(articleResponse);
    assertThat(article.get("slug")).isEqualTo("hello-zblog2");
    assertThat(article.get("content")).asString().contains("<h1");

    assertThat(randomResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(randomResponse)).isEqualTo("hello-zblog2");
  }

  @Test
  void adminCanCreateUpdateAndPublishArticleWithTaxonomy() {
    HttpHeaders headers = authenticatedHeaders();

    ResponseEntity<Map> categoryResponse =
        restTemplate.exchange(
            "/api/v1/admin/categories",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("name", "Java", "slug", "java", "description", "Java notes", "sort", 10),
                headers),
            Map.class);
    assertThat(categoryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Number categoryId = (Number) ((Map<?, ?>) data(categoryResponse)).get("id");

    ResponseEntity<Map> tagResponse =
        restTemplate.exchange(
            "/api/v1/admin/tags",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "DDD", "slug", "ddd", "description", "Domain design"), headers),
            Map.class);
    assertThat(tagResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Number tagId = (Number) ((Map<?, ?>) data(tagResponse)).get("id");

    ResponseEntity<Map> articleResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title",
                    "Phase 3 Article",
                    "slug",
                    "phase-3-article",
                    "content",
                    "# Phase 3\n\nJava backend content.",
                    "summary",
                    "Phase 3 summary",
                    "category_id",
                    categoryId,
                    "tag_ids",
                    List.of(tagId),
                    "is_top",
                    false,
                    "is_essence",
                    true),
                headers),
            Map.class);
    assertThat(articleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> article = (Map<?, ?>) data(articleResponse);
    Number articleId = (Number) article.get("id");
    assertThat(article.get("is_publish")).isEqualTo(false);

    ResponseEntity<Map> publishResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/publish",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Map<?, ?>) data(publishResponse)).get("is_publish")).isEqualTo(true);

    ResponseEntity<Map> publicArticleResponse =
        restTemplate.getForEntity("/api/v1/articles/phase-3-article", Map.class);
    assertThat(publicArticleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Map<?, ?>) data(publicArticleResponse)).get("title")).isEqualTo("Phase 3 Article");
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);
    String accessToken =
        (String) ((Map<?, ?>) data(loginResponse)).get("access_token");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private Object data(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }
}
