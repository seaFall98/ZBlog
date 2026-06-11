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
class FoundationApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void healthEndpointUsesUnifiedResponseEnvelope() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/health", Map.class);
    Map<?, ?> body = response.getBody();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    assertThat(body.get("message")).isEqualTo("success");
    assertThat(((Map<?, ?>) body.get("data")).get("status")).isEqualTo("UP");
  }

  @Test
  void loginReturnsBearerTokenThatCanAccessProtectedPing() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> loginBody = loginResponse.getBody();
    assertThat(loginBody).isNotNull();
    String accessToken =
        (String) ((Map<?, ?>) loginBody.get("data")).get("access_token");
    assertThat(accessToken).isNotBlank();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<Map> pingResponse =
        restTemplate.exchange(
            "/api/v1/admin/ping", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(pingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> pingBody = pingResponse.getBody();
    assertThat(pingBody).isNotNull();
    assertThat(pingBody.get("code")).isEqualTo(0);
    assertThat(((Map<?, ?>) pingBody.get("data")).get("message")).isEqualTo("pong");
  }

  @Test
  void loginTokenCanFetchCurrentUserProfile() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);

    String accessToken =
        (String) ((Map<?, ?>) loginResponse.getBody().get("data")).get("access_token");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<Map> profileResponse =
        restTemplate.exchange(
            "/api/v1/user/profile", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> profileBody = profileResponse.getBody();
    assertThat(profileBody).isNotNull();
    assertThat(profileBody.get("code")).isEqualTo(0);
    Map<?, ?> profile = (Map<?, ?>) profileBody.get("data");
    assertThat(profile.get("role")).isEqualTo("super_admin");
    assertThat(profile.get("nickname")).isEqualTo("admin");
    assertThat(profile.get("has_password")).isEqualTo(true);
  }

  @Test
  void loginAlsoAcceptsLegacyEmailField() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", "admin", "password", "admin123456"),
            Map.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> loginBody = loginResponse.getBody();
    assertThat(loginBody).isNotNull();
    assertThat(loginBody.get("code")).isEqualTo(0);
    assertThat(((Map<?, ?>) loginBody.get("data")).get("access_token")).isNotNull();
  }

  @Test
  void protectedPingRejectsMissingToken() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/admin/ping", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(401);
  }

  @Test
  void publicMomentsEndpointReturnsPageEnvelope() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/moments", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> page = (Map<?, ?>) data(response);
    List<String> keys = page.keySet().stream().map(String::valueOf).toList();
    assertThat(keys).containsAll(List.of("list", "total", "page", "page_size"));
  }

  @Test
  void publicMenusDoNotExposeSmokeAndFooterContainsFeedback() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/menus", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String menus = data(response).toString();
    assertThat(menus).doesNotContain("Smoke", "/smoke");
    assertThat(menus).contains("反馈投诉", "/feedback");
    assertThat(menus).doesNotContain("aggregate");
  }

  private Object data(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }
}
