package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zblog.common.exception.BusinessException;
import com.zblog.identity.application.UserService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class P3AuthApiTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserService userService;

  @Test
  void registerAutoLogsInAndTokenCanReadProfile() {
    String email = "p3-register-" + System.nanoTime() + "@example.com";

    ResponseEntity<Map> registerResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", email, "nickname", "P3 Reader", "password", "reader123456"),
            Map.class);

    assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> registerData = data(registerResponse);
    assertThat(registerData.get("access_token")).isInstanceOf(String.class);
    assertThat(((Map<?, ?>) registerData.get("user")).get("email")).isEqualTo(email);

    var headers = new org.springframework.http.HttpHeaders();
    headers.setBearerAuth(registerData.get("access_token").toString());
    ResponseEntity<Map> profileResponse =
        restTemplate.exchange(
            "/api/v1/user/profile",
            org.springframework.http.HttpMethod.GET,
            new org.springframework.http.HttpEntity<>(headers),
            Map.class);

    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(profileResponse).get("email")).isEqualTo(email);
  }

  @Test
  void forgotPasswordDoesNotRevealWhetherAccountExists() {
    String email = "p3-reset-" + System.nanoTime() + "@example.com";
    register(email, "Reset Reader", "reader123456");

    ResponseEntity<Map> existingResponse =
        restTemplate.postForEntity("/api/v1/auth/forgot-password", Map.of("email", email), Map.class);
    ResponseEntity<Map> missingResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/forgot-password",
            Map.of("email", "missing-" + System.nanoTime() + "@example.com"),
            Map.class);

    assertThat(existingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(missingResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(existingResponse.getBody()).isEqualTo(missingResponse.getBody());
    assertThat(resetTokenCount(email)).isEqualTo(1L);
  }

  @Test
  void loginLocksAfterFiveFailures() {
    String email = "p3-lock-" + System.nanoTime() + "@example.com";
    register(email, "Lock Reader", "reader123456");

    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(() -> userService.login(email, "wrong-password", "phase-a-test"))
          .isInstanceOf(BusinessException.class)
          .satisfies(
              exception ->
                  assertThat(((BusinessException) exception).status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    assertThatThrownBy(() -> userService.login(email, "reader123456", "phase-a-test"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception ->
                assertThat(((BusinessException) exception).status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
  }

  private void register(String email, String nickname, String password) {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", email, "nickname", nickname, "password", password),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private long resetTokenCount(String email) {
    return jdbcTemplate.queryForObject(
        "select count(*) from password_reset_tokens where email = ?", Long.class, email);
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(0);
    return (Map<?, ?>) response.getBody().get("data");
  }
}
