package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.zblog.mail.MailProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
  @Autowired private MailProperties mailProperties;

  @org.springframework.boot.test.web.server.LocalServerPort private int port;

  @BeforeEach
  void cleanBatch18MailRecords() {
    jdbcTemplate.update("delete from mail_outbox where recipient like ?", "mail-%-batch18@example.com");
  }

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
  void mailFlowRecordsFailedStatusWhenAutomaticDeliveryFails() {
    String previousMode = mailProperties.getMode();
    String previousHost = mailProperties.getHost();
    try {
      mailProperties.setMode("smtp");
      mailProperties.setHost("");

      ResponseEntity<Map> forgotResponse =
          restTemplate.postForEntity(
              "/api/v1/auth/forgot-password", Map.of("email", "reset-batch3@example.com"), Map.class);

      assertThat(forgotResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(data(forgotResponse).get("sent")).isEqualTo(true);
    } finally {
      mailProperties.setMode(previousMode);
      mailProperties.setHost(previousHost);
    }

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            select status, attempts, last_attempt_at, next_attempt_at, error_message
            from mail_outbox
            where recipient = ? and mail_type = 'password_reset'
            order by id desc
            limit 1
            """,
            "reset-batch3@example.com");
    assertThat(row.get("status")).isEqualTo("failed");
    assertThat(row.get("attempts")).isEqualTo(1);
    assertThat(row.get("last_attempt_at")).isNotNull();
    assertThat(row.get("next_attempt_at")).isNotNull();
    assertThat(row.get("error_message").toString()).contains("SMTP host is not configured");
  }

  @Test
  void adminMailOutboxListsAndDrainsPendingMail() {
    HttpHeaders headers = authenticatedHeaders();
    jdbcTemplate.update(
        """
        insert into mail_outbox (audience, mail_type, recipient, subject, body, status, created_at)
        values (?, ?, ?, ?, ?, 'pending', timestamp '2000-01-01 00:00:00')
        """,
        "user",
        "batch18_drain_success",
        "mail-success-batch18@example.com",
        "Batch18 success",
        "Mail drain succeeds locally");

    Map<?, ?> pending = adminGet("/api/v1/admin/mail-outbox?status=pending", headers);
    assertThat((Integer) pending.get("total")).isGreaterThanOrEqualTo(1);
    assertThat((List<?>) pending.get("list"))
        .anySatisfy(
            item -> {
              Map<?, ?> mail = (Map<?, ?>) item;
              assertThat(mail.get("recipient")).isEqualTo("mail-success-batch18@example.com");
              assertThat(mail.get("status")).isEqualTo("pending");
              assertThat(mail.get("created_at").toString()).startsWith("2000-01-01 00:00:00");
              assertThat(mail.get("created_at").toString()).doesNotContain("T").doesNotContain("Z");
            });

    Map<?, ?> drain =
        data(
            restTemplate.exchange(
                "/api/v1/admin/mail-outbox/drain?limit=1",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class));

    assertThat(drain.get("total")).isEqualTo(1);
    assertThat(drain.get("sent")).isEqualTo(1);
    assertThat(drain.get("failed")).isEqualTo(0);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select status, attempts, sent_at, error_message from mail_outbox where recipient = ?",
            "mail-success-batch18@example.com");
    assertThat(row.get("status")).isEqualTo("sent");
    assertThat(row.get("attempts")).isEqualTo(1);
    assertThat(row.get("sent_at")).isNotNull();
    assertThat(row.get("error_message")).isNull();
  }

  @Test
  void adminMailOutboxDrainRecordsFailureForRetry() {
    HttpHeaders headers = authenticatedHeaders();
    jdbcTemplate.update(
        """
        insert into mail_outbox (audience, mail_type, recipient, subject, body, status, created_at)
        values (?, ?, ?, ?, ?, 'pending', timestamp '2000-01-01 00:00:00')
        """,
        "user",
        "batch18_drain_failure",
        "mail-failure-batch18@example.com",
        "Batch18 failure",
        "Mail drain records local SMTP configuration failure");

    String previousMode = mailProperties.getMode();
    try {
      mailProperties.setMode("smtp");
      Map<?, ?> drain =
          data(
              restTemplate.exchange(
                  "/api/v1/admin/mail-outbox/drain?limit=1",
                  HttpMethod.POST,
                  new HttpEntity<>(headers),
                  Map.class));

      assertThat(drain.get("total")).isEqualTo(1);
      assertThat(drain.get("sent")).isEqualTo(0);
      assertThat(drain.get("failed")).isEqualTo(1);
    } finally {
      mailProperties.setMode(previousMode);
    }

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            select status, attempts, last_attempt_at, next_attempt_at, error_message
            from mail_outbox
            where recipient = ?
            """,
            "mail-failure-batch18@example.com");
    assertThat(row.get("status")).isEqualTo("failed");
    assertThat(row.get("attempts")).isEqualTo(1);
    assertThat(row.get("last_attempt_at")).isNotNull();
    assertThat(row.get("next_attempt_at")).isNotNull();
    assertThat(row.get("error_message").toString()).contains("SMTP host is not configured");
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

  private Map<?, ?> adminGet(String path, HttpHeaders headers) {
    return data(restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class));
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return (Map<?, ?>) body.get("data");
  }
}
