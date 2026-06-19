package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
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
class P4SubscriptionDeliveryTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clean() {
    jdbcTemplate.update("delete from mail_outbox where recipient like ?", "p4-subscription-%@example.com");
    jdbcTemplate.update("delete from subscribers where email like ?", "p4-subscription-%@example.com");
    jdbcTemplate.update("delete from event_consumptions");
    jdbcTemplate.update("delete from event_outbox");
    jdbcTemplate.update("delete from articles where slug like ?", "p4-subscription-%");
  }

  @Test
  void doubleOptInSubscriberReceivesMailOnlyAfterConfirming() {
    String activeEmail = "p4-subscription-active@example.com";
    String pendingEmail = "p4-subscription-pending@example.com";

    Map<?, ?> activeSubscribe = data(restTemplate.postForEntity("/api/v1/subscribe", Map.of("email", activeEmail), Map.class));
    assertThat(activeSubscribe.get("status")).isEqualTo("PENDING");
    assertThat(activeSubscribe.keySet().stream().map(Object::toString).toList())
        .doesNotContain("confirmation_token", "unsubscribe_token");

    data(restTemplate.postForEntity("/api/v1/subscribe", Map.of("email", pendingEmail), Map.class));

    String confirmToken =
        jdbcTemplate.queryForObject(
            "select confirmation_token from subscribers where email = ?", String.class, activeEmail);
    ResponseEntity<Map> confirmResponse =
        restTemplate.getForEntity("/api/v1/subscribe/confirm?token=" + confirmToken, Map.class);
    assertThat(confirmResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(confirmResponse).get("status")).isEqualTo("ACTIVE");

    long articleId =
        createArticle(authenticatedHeaders(), "p4-subscription-" + UUID.randomUUID(), "P4 Subscription Article", true);
    data(
        restTemplate.exchange(
            "/api/v1/admin/outbox/publish-pending",
            HttpMethod.POST,
            new HttpEntity<>(authenticatedHeaders()),
            Map.class));

    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*) from mail_outbox
                where recipient = ? and mail_type = 'article_published'
                  and subject like ?
                """,
                Long.class,
                activeEmail,
                "%P4 Subscription Article%"))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from mail_outbox where recipient = ? and mail_type = 'article_published'",
                Long.class,
                pendingEmail))
        .isZero();

    String activeStatus =
        jdbcTemplate.queryForObject("select last_delivery_status from subscribers where email = ?", String.class, activeEmail);
    assertThat(activeStatus).isEqualTo("queued");
    assertThat(articleId).isPositive();
  }

  @Test
  void publicSubscribeDoesNotDowngradeActiveSubscriberOrExposeTokens() {
    String email = "p4-subscription-existing-active@example.com";

    data(restTemplate.postForEntity("/api/v1/subscribe", Map.of("email", email), Map.class));
    String confirmToken =
        jdbcTemplate.queryForObject(
            "select confirmation_token from subscribers where email = ?", String.class, email);
    data(restTemplate.getForEntity("/api/v1/subscribe/confirm?token=" + confirmToken, Map.class));

    Map<?, ?> resubscribe =
        data(restTemplate.postForEntity("/api/v1/subscribe", Map.of("email", email), Map.class));

    assertThat(resubscribe.get("status")).isEqualTo("ACTIVE");
    assertThat(resubscribe.get("active")).isEqualTo(true);
    assertThat(resubscribe.keySet().stream().map(Object::toString).toList())
        .doesNotContain("confirmation_token", "unsubscribe_token");
    assertThat(
            jdbcTemplate.queryForObject(
                "select status from subscribers where email = ?", String.class, email))
        .isEqualTo("ACTIVE");
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select count(*) from mail_outbox
                where recipient = ? and mail_type = 'subscribe_confirm'
                """,
                Long.class,
                email))
        .isEqualTo(1L);
  }

  @Test
  void publicSubscribeRestoresSoftDeletedSubscriberWithoutExposingTokens() {
    String email = "p4-subscription-soft-deleted@example.com";
    jdbcTemplate.update(
        """
        insert into subscribers (email, unsubscribe_token, active, status, confirmation_token, deleted_at)
        values (?, 'p4-soft-deleted-unsubscribe-token', false, 'UNSUBSCRIBED',
          'p4-soft-deleted-confirmation-token', current_timestamp)
        """,
        email);

    Map<?, ?> resubscribe =
        data(restTemplate.postForEntity("/api/v1/subscribe", Map.of("email", email), Map.class));

    assertThat(resubscribe.get("status")).isEqualTo("PENDING");
    assertThat(resubscribe.keySet().stream().map(Object::toString).toList())
        .doesNotContain("confirmation_token", "unsubscribe_token");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from subscribers where email = ? and deleted_at is null",
                Long.class,
                email))
        .isEqualTo(1L);
  }

  private long createArticle(HttpHeaders headers, String slug, String title, boolean publish) {
    Map<?, ?> article =
        data(
            restTemplate.exchange(
                "/api/v1/admin/articles",
                HttpMethod.POST,
                new HttpEntity<>(
                    Map.of(
                        "title",
                        title,
                        "slug",
                        slug,
                        "content_markdown",
                        "Subscription test article body",
                        "summary",
                        "Subscription summary",
                        "is_publish",
                        publish),
                    headers),
                Map.class));
    return ((Number) article.get("id")).longValue();
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
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return (Map<?, ?>) body.get("data");
  }
}
