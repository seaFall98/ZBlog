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
class P1FeedbackSubscriptionRssApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void feedbackCanBeSubmittedQueriedAndHandledByAdmin() {
    ResponseEntity<Map> submitResponse =
        restTemplate.postForEntity(
            "/api/v1/feedback",
            Map.of(
                "reportUrl",
                "https://example.com/posts/demo",
                "reportType",
                "suggestion",
                "email",
                "feedback@example.com",
                "description",
                "Please improve this page"),
            Map.class);

    assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> submitted = data(submitResponse);
    assertThat(submitted.get("ticket_no")).isNotNull();
    assertThat(((Map<?, ?>) submitted.get("form_content")).get("description"))
        .isEqualTo("Please improve this page");

    ResponseEntity<Map> ticketResponse =
        restTemplate.getForEntity("/api/v1/feedback/ticket/" + submitted.get("ticket_no"), Map.class);
    Map<?, ?> ticket = data(ticketResponse);
    assertThat(ticket.get("ticket_no")).isEqualTo(submitted.get("ticket_no"));
    assertThat(ticket.keySet().stream().map(Object::toString).toList())
        .doesNotContain("access_token", "form_content", "messages", "ip", "user_agent");

    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/feedback?keyword=feedback@example.com&page=1&page_size=10",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    Map<?, ?> page = data(listResponse);
    List<?> list = (List<?>) page.get("list");
    assertThat(list).isNotEmpty();
    Number id = (Number) ((Map<?, ?>) list.getFirst()).get("id");

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/admin/feedback/" + id,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("status", "resolved", "admin_reply", "Done"), headers),
            Map.class);
    Map<?, ?> updated = data(updateResponse);
    assertThat(updated.get("status")).isEqualTo("resolved");
    assertThat(updated.get("admin_reply")).isEqualTo("Done");
  }

  @Test
  void subscriptionCanBeCreatedListedUnsubscribedAndDeleted() {
    ResponseEntity<Map> subscribeResponse =
        restTemplate.postForEntity(
            "/api/v1/subscribe", Map.of("email", "subscriber@example.com"), Map.class);
    assertThat(subscribeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> subscribed = data(subscribeResponse);
    assertThat(subscribed.get("active")).isEqualTo(false);
    assertThat(subscribed.get("status")).isEqualTo("PENDING");
    assertThat(subscribed.keySet().stream().map(Object::toString).toList())
        .doesNotContain("confirmation_token", "unsubscribe_token");

    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/subscribers?page=1&page_size=20",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    Map<?, ?> page = data(listResponse);
    List<?> list = (List<?>) page.get("list");
    assertThat(list)
        .anySatisfy(
            item -> assertThat(((Map<?, ?>) item).get("email")).isEqualTo("subscriber@example.com"));
    Number id =
        (Number)
            list.stream()
                .map(Map.class::cast)
                .filter(item -> item.get("email").equals("subscriber@example.com"))
                .findFirst()
                .orElseThrow()
                .get("id");

    ResponseEntity<Map> unsubscribeResponse =
        restTemplate.getForEntity("/api/v1/subscribe/unsubscribe?token=seed-reader-token", Map.class);
    assertThat(data(unsubscribeResponse).get("active")).isEqualTo(false);

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/subscribers/" + id,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void adminRssFeedCanBeListedAndMarkedRead() {
    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/rssfeed?page=1&page_size=20&is_read=false",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    Map<?, ?> page = data(listResponse);
    assertThat(((Number) page.get("unread_count")).longValue()).isGreaterThanOrEqualTo(1);
    List<?> list = (List<?>) page.get("list");
    assertThat(list).isNotEmpty();
    Number id = (Number) ((Map<?, ?>) list.getFirst()).get("id");

    ResponseEntity<Map> markResponse =
        restTemplate.exchange(
            "/api/v1/admin/rssfeed/" + id + "/read",
            HttpMethod.PUT,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(markResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> allResponse =
        restTemplate.exchange(
            "/api/v1/admin/rssfeed/read-all", HttpMethod.PUT, new HttpEntity<>(headers), Map.class);
    assertThat(data(allResponse).get("affected")).isNotNull();
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
