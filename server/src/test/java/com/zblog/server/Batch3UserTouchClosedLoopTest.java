package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
class Batch3UserTouchClosedLoopTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @org.springframework.boot.test.web.server.LocalServerPort private int port;

  @Test
  void feedbackSubscribeAndUnsubscribeCreateDurableMailRecords() {
    ResponseEntity<Map> feedbackResponse =
        restTemplate.postForEntity(
            "/api/v1/feedback",
            Map.of(
                "reportUrl",
                "https://example.com/posts/batch3",
                "reportType",
                "suggestion",
                "email",
                "feedback-batch3@example.com",
                "description",
                "Please notify the owner"),
            Map.class);
    assertThat(feedbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> feedback = data(feedbackResponse);

    assertMail(
        "admin",
        "feedback_new",
        "zz1362410372@qq.com",
        feedback.get("ticket_no").toString(),
        "Please notify the owner");

    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> replyResponse =
        restTemplate.exchange(
            "/api/v1/admin/feedback/" + feedback.get("id"),
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("status", "resolved", "admin_reply", "Thanks, fixed"), headers),
            Map.class);
    assertThat(replyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertMail(
        "user",
        "feedback_reply",
        "feedback-batch3@example.com",
        feedback.get("ticket_no").toString(),
        "Thanks, fixed");

    ResponseEntity<Map> subscribeResponse =
        restTemplate.postForEntity("/api/v1/subscribe", Map.of("email", "batch3-subscriber@example.com"), Map.class);
    assertThat(subscribeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    String token =
        jdbcTemplate.queryForObject(
            "select unsubscribe_token from subscribers where email = ?", String.class, "batch3-subscriber@example.com");
    assertMail("user", "subscribe_confirm", "batch3-subscriber@example.com", "unsubscribe", token);

    ResponseEntity<Map> unsubscribeResponse =
        restTemplate.getForEntity("/api/v1/subscribe/unsubscribe?token=" + token, Map.class);
    assertThat(unsubscribeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertMail("user", "unsubscribe_confirm", "batch3-subscriber@example.com", "已退订", "batch3-subscriber@example.com");
  }

  @Test
  void forgotPasswordResetChangesPasswordAndTokenIsSingleUse() throws Exception {
    restTemplate.postForEntity(
        "/api/v1/auth/register",
        Map.of("email", "reset-batch3@example.com", "nickname", "Reset Batch3", "password", "oldPass123"),
        Map.class);

    ResponseEntity<Map> forgotResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/forgot-password", Map.of("email", "reset-batch3@example.com"), Map.class);
    assertThat(forgotResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(forgotResponse).get("sent")).isEqualTo(true);

    String token =
        jdbcTemplate.queryForObject(
            "select token from password_reset_tokens where email = ? and used_at is null",
            String.class,
            "reset-batch3@example.com");
    assertMail("user", "password_reset", "reset-batch3@example.com", "重置密码", token);

    ResponseEntity<Map> resetResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/reset-password",
            Map.of("email", "reset-batch3@example.com", "code", token, "password", "newPass123"),
            Map.class);
    assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(loginStatus("reset-batch3@example.com", "oldPass123")).isEqualTo(401);
    assertThat(
            restTemplate.postForEntity(
                "/api/v1/auth/login",
                Map.of("email", "reset-batch3@example.com", "password", "newPass123"),
                Map.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> reuseResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/reset-password",
            Map.of("email", "reset-batch3@example.com", "code", token, "password", "anotherPass123"),
            Map.class);
    assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void oauthBeginIsExplicitlyUnsupportedWithoutProviderCredentials() {
    ResponseEntity<Map> oauthResponse =
        restTemplate.getForEntity("/api/v1/auth/github?redirect=%2F", Map.class);

    assertThat(oauthResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    Map<?, ?> body = oauthResponse.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(501);
    assertThat(body.get("message").toString()).contains("OAuth").contains("unsupported");
  }

  private void assertMail(String audience, String type, String recipient, String... contentParts) {
    List<Map<String, Object>> mails =
        jdbcTemplate.queryForList(
            """
            select * from mail_outbox
            where audience = ? and mail_type = ? and recipient = ?
            order by id desc
            """,
            audience,
            type,
            recipient);
    assertThat(mails).isNotEmpty();
    String subject = mails.getFirst().get("subject").toString();
    String body = mails.getFirst().get("body").toString();
    assertThat(mails.getFirst().get("status")).isEqualTo("sent");
    for (String part : contentParts) {
      assertThat(subject + "\n" + body).contains(part);
    }
  }

  private int loginStatus(String email, String password) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
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
