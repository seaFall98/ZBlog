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
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MomentApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void adminMomentsCanBeCreatedListedUpdatedAndDeleted() {
    HttpHeaders headers = authenticatedHeaders();

    ResponseEntity<Map> createResponse =
        restTemplate.exchange(
            "/api/v1/admin/moments",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "content",
                    Map.of(
                        "text", "Phase6 moment",
                        "tags", "phase6,backend",
                        "images", List.of("https://example.com/moment.png"),
                        "location", "Hangzhou",
                        "link",
                        Map.of(
                            "url", "https://example.com",
                            "title", "Example",
                            "favicon", "https://example.com/favicon.ico")),
                    "is_publish", true,
                    "publish_time", "2026-05-15 10:00:00"),
                headers),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> created = (Map<?, ?>) data(createResponse);
    Number momentId = (Number) created.get("id");
    Map<?, ?> createdContent = (Map<?, ?>) created.get("content");
    assertThat(createdContent.get("text")).isEqualTo("Phase6 moment");
    assertThat(((List<?>) createdContent.get("images"))).hasSize(1);

    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/moments?keyword=Phase6&tags=backend&location=Hangzhou&has_images=true&is_publish=true",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    Map<?, ?> page = (Map<?, ?>) data(listResponse);
    assertThat((List<?>) page.get("list")).hasSize(1);

    ResponseEntity<Map> getResponse =
        restTemplate.exchange(
            "/api/v1/admin/moments/" + momentId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(((Map<?, ?>) data(getResponse)).get("id")).isEqualTo(momentId);

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/admin/moments/" + momentId,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("content", Map.of("text", "Phase6 moment updated"), "is_publish", false), headers),
            Map.class);
    Map<?, ?> updated = (Map<?, ?>) data(updateResponse);
    assertThat(updated.get("is_publish")).isEqualTo(false);
    assertThat(((Map<?, ?>) updated.get("content")).get("text")).isEqualTo("Phase6 moment updated");

    ResponseEntity<Map> publicResponse = restTemplate.getForEntity("/api/v1/moments", Map.class);
    Map<?, ?> publicPage = (Map<?, ?>) data(publicResponse);
    List<?> publicList = (List<?>) publicPage.get("list");
    assertThat(publicList)
        .noneMatch(item -> momentId.equals(((Map<?, ?>) item).get("id")));

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/moments/" + momentId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void publicMomentsExposePageEnvelope() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/moments", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> page = (Map<?, ?>) data(response);
    List<String> keys = page.keySet().stream().map(String::valueOf).toList();
    assertThat(keys).containsAll(List.of("list", "total", "page", "page_size"));
  }

  @Test
  void statsCountMoments() {
    HttpHeaders headers = authenticatedHeaders();
    restTemplate.exchange(
        "/api/v1/admin/moments",
        HttpMethod.POST,
        new HttpEntity<>(
            Map.of(
                "content",
                Map.of("text", "Stats moment"),
                "is_publish", true),
            headers),
        Map.class);

    ResponseEntity<Map> statsResponse = restTemplate.getForEntity("/api/v1/stats/site", Map.class);
    Map<?, ?> stats = (Map<?, ?>) data(statsResponse);
    assertThat(((Number) stats.get("total_moments")).longValue()).isGreaterThanOrEqualTo(1);
    restTemplate.exchange(
        "/api/v1/admin/moments/1",
        HttpMethod.DELETE,
        new HttpEntity<>(headers),
        Map.class);
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);
    String accessToken = (String) ((Map<?, ?>) data(loginResponse)).get("access_token");
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
