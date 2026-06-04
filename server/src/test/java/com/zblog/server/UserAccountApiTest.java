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
class UserAccountApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void profileCanBeUpdatedAndReadBack() {
    HttpHeaders headers = authHeaders("admin", "admin123456");

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/user/profile",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of("nickname", "站长", "email", "admin", "website", "https://example.com"),
                headers),
            Map.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> updated = data(updateResponse);
    assertThat(updated.get("nickname")).isEqualTo("站长");
    assertThat(updated.get("website")).isEqualTo("https://example.com");

    ResponseEntity<Map> profileResponse =
        restTemplate.exchange(
            "/api/v1/user/profile", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    Map<?, ?> profile = data(profileResponse);
    assertThat(profile.get("nickname")).isEqualTo("站长");
    assertThat(profile.get("has_password")).isEqualTo(true);
    assertThat((List<?>) profile.get("linked_oauths")).isEmpty();
  }

  @Test
  void registrationCreatesRealUserThatCanLogin() {
    ResponseEntity<Map> registerResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            Map.of(
                "email",
                "reader@example.com",
                "nickname",
                "Reader",
                "password",
                "reader123456"),
            Map.class);

    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> registerData = data(registerResponse);
    assertThat(registerData.get("access_token")).isNotNull();
    assertThat(((Map<?, ?>) registerData.get("user")).get("email")).isEqualTo("reader@example.com");

    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", "reader@example.com", "password", "reader123456"),
            Map.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(loginResponse).get("access_token")).isNotNull();
  }

  @Test
  void adminUsersCrudRoundTripsThroughDatabase() {
    HttpHeaders headers = authHeaders("admin", "admin123456");

    ResponseEntity<Map> createResponse =
        restTemplate.exchange(
            "/api/v1/admin/users",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "email",
                    "editor@example.com",
                    "nickname",
                    "Editor",
                    "password",
                    "editor123456",
                    "role",
                    "admin"),
                headers),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Number id = (Number) data(createResponse).get("id");

    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/users?keyword=editor&page=1&page_size=10",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);

    Map<?, ?> page = data(listResponse);
    assertThat(((Number) page.get("total")).longValue()).isGreaterThanOrEqualTo(1);
    List<?> list = (List<?>) page.get("list");
    assertThat(list).anySatisfy(item -> assertThat(((Map<?, ?>) item).get("email")).isEqualTo("editor@example.com"));

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/admin/users/" + id,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("nickname", "Editor Updated", "is_enabled", true), headers),
            Map.class);

    assertThat(data(updateResponse).get("nickname")).isEqualTo("Editor Updated");

    ResponseEntity<Map> resetResponse =
        restTemplate.exchange(
            "/api/v1/admin/users/" + id + "/password",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("new_password", "newEditor123"), headers),
            Map.class);
    assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", "editor@example.com", "password", "newEditor123"),
            Map.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/users/" + id,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> deletedUserResponse =
        restTemplate.exchange(
            "/api/v1/admin/users/" + id,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    Map<?, ?> deletedUser = data(deletedUserResponse);
    assertThat(deletedUser.get("is_enabled")).isEqualTo(false);
    assertThat(deletedUser.get("deleted_at")).isNotNull();
  }

  private HttpHeaders authHeaders(String username, String password) {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", username, "password", password), Map.class);
    String token = (String) data(loginResponse).get("access_token");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return (Map<?, ?>) body.get("data");
  }
}
