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
class P4FeedbackBackendTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void feedbackTicketTimelineSupportsAdminReplyUserSupplementAndMine() {
    String email = "p4-feedback-" + System.nanoTime() + "@example.com";
    HttpHeaders userHeaders = registerHeaders(email);

    Map<?, ?> submitted =
        data(
            restTemplate.exchange(
                "/api/v1/feedback",
                HttpMethod.POST,
                new HttpEntity<>(
                    Map.of(
                        "reportUrl", "/posts/p4-feedback",
                        "reportType", "suggestion",
                        "description", "The feedback page should be useful"),
                    userHeaders),
                Map.class));

    assertThat(submitted.get("ticket_no")).isNotNull();
    assertThat(submitted.get("access_token")).isNotNull();
    assertThat(submitted.get("status")).isEqualTo("PENDING");
    assertThat((List<?>) submitted.get("messages")).hasSize(1);

    Map<?, ?> ticketSummary =
        data(
            restTemplate.getForEntity(
                "/api/v1/feedback/ticket/" + submitted.get("ticket_no"), Map.class));
    assertThat(ticketSummary.get("ticket_no")).isEqualTo(submitted.get("ticket_no"));
    assertThat(ticketSummary.get("status")).isEqualTo("PENDING");
    assertThat(ticketSummary.keySet().stream().map(Object::toString).toList())
        .doesNotContain("access_token", "form_content", "messages", "email", "ip", "user_agent");

    Map<?, ?> tokenDetail =
        data(
            restTemplate.getForEntity(
                "/api/v1/feedback/token/" + submitted.get("access_token"), Map.class));
    assertThat((List<?>) tokenDetail.get("messages")).hasSize(1);

    Map<?, ?> mine =
        data(
            restTemplate.exchange(
                "/api/v1/feedback/mine?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                Map.class));
    assertThat((List<?>) mine.get("list"))
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("ticket_no")).isEqualTo(submitted.get("ticket_no")));

    HttpHeaders adminHeaders = adminHeaders();
    Map<?, ?> waiting =
        data(
            restTemplate.exchange(
                "/api/v1/admin/feedback/" + submitted.get("id") + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", "WAITING_USER", "content", "Please add a screenshot"), adminHeaders),
                Map.class));
    assertThat(waiting.get("status")).isEqualTo("WAITING_USER");

    Map<?, ?> supplemented =
        data(
            restTemplate.exchange(
                "/api/v1/feedback/" + submitted.get("id") + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("content", "Screenshot attached soon"), userHeaders),
                Map.class));
    assertThat(supplemented.get("status")).isEqualTo("IN_PROGRESS");
    assertThat((List<?>) supplemented.get("messages"))
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("content")).isEqualTo("Screenshot attached soon"));

    Map<?, ?> replied =
        data(
            restTemplate.exchange(
                "/api/v1/admin/feedback/" + submitted.get("id") + "/messages",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("content", "Thanks, we will polish it"), adminHeaders),
                Map.class));
    assertThat((List<?>) replied.get("messages"))
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("actor_type")).isEqualTo("ADMIN"));
    assertMail("user", "feedback_reply", email, "Thanks, we will polish it");

    Map<?, ?> closed =
        data(
            restTemplate.exchange(
                "/api/v1/admin/feedback/" + submitted.get("id") + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("status", "CLOSED"), adminHeaders),
                Map.class));
    assertThat(closed.get("status")).isEqualTo("CLOSED");

    ResponseEntity<Map> blocked =
        restTemplate.exchange(
            "/api/v1/feedback/" + submitted.get("id") + "/messages",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("content", "Please reopen"), userHeaders),
            Map.class);
    assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void feedbackCleanupOnlyDeletesOldResolvedOrClosedTickets() {
    jdbcTemplate.update(
        """
        insert into feedbacks (ticket_no, access_token, report_url, report_type, form_content, email, status, feedback_time, updated_at, resolved_at)
        values ('P4OLDRESOLVED', 'p4-old-resolved-token', '/old', 'suggestion', '{}', '', 'RESOLVED',
          current_timestamp - interval '100' day, current_timestamp - interval '100' day, current_timestamp - interval '100' day)
        """);
    jdbcTemplate.update(
        """
        insert into feedbacks (ticket_no, access_token, report_url, report_type, form_content, email, status, feedback_time, updated_at)
        values ('P4OLDPENDING', 'p4-old-pending-token', '/old', 'suggestion', '{}', '', 'PENDING',
          current_timestamp - interval '100' day, current_timestamp - interval '100' day)
        """);

    Map<?, ?> result =
        data(
            restTemplate.exchange(
                "/api/v1/admin/scheduled-jobs",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders()),
                Map.class));
    Number cleanupId =
        (Number)
            ((List<?>) result.get("list"))
                .stream()
                    .map(Map.class::cast)
                    .filter(row -> "feedback-cleanup".equals(row.get("handler_name")))
                    .findFirst()
                    .orElseThrow()
                    .get("id");

    restTemplate.exchange(
        "/api/v1/admin/scheduled-jobs/" + cleanupId + "/run",
        HttpMethod.POST,
        new HttpEntity<>(adminHeaders()),
        Map.class);

    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from feedbacks where ticket_no = 'P4OLDRESOLVED' and deleted_at is not null",
                Long.class))
        .isEqualTo(1L);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from feedbacks where ticket_no = 'P4OLDPENDING' and deleted_at is null",
                Long.class))
        .isEqualTo(1L);
  }

  private HttpHeaders registerHeaders(String email) {
    Map<?, ?> register =
        data(
            restTemplate.postForEntity(
                "/api/v1/auth/register",
                Map.of("email", email, "nickname", "P4 Feedback", "password", "reader123456"),
                Map.class));
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(register.get("access_token").toString());
    return headers;
  }

  private HttpHeaders adminHeaders() {
    Map<?, ?> login =
        data(restTemplate.postForEntity("/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class));
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(login.get("access_token").toString());
    return headers;
  }

  private void assertMail(String audience, String type, String recipient, String contentPart) {
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
    assertThat(mails.getFirst().get("body").toString()).contains(contentPart);
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(0);
    return (Map<?, ?>) response.getBody().get("data");
  }
}
