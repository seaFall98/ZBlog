package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.zblog.event.application.EventOutboxService;
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
class P3NotificationApiTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private EventOutboxService eventOutboxService;

  @Test
  void commentReplyCreatesRecipientNotificationAndUnreadCountCanBeCleared() {
    String targetKey = "p3-notification-" + System.nanoTime();
    HttpHeaders ownerHeaders = registerAndAuth("p3-notify-owner-" + System.nanoTime() + "@example.com", "Notify Owner");
    HttpHeaders actorHeaders = registerAndAuth("p3-notify-actor-" + System.nanoTime() + "@example.com", "Notify Actor");

    long parentId =
        number(
            data(
                restTemplate.exchange(
                    "/api/v1/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(
                        Map.of("target_type", "article", "target_key", targetKey, "content", "root comment"),
                        ownerHeaders),
                    Map.class)),
            "id");
    long replyId =
        number(
            data(
                restTemplate.exchange(
                    "/api/v1/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(
                        Map.of(
                            "target_type",
                            "article",
                            "target_key",
                            targetKey,
                            "parent_id",
                            parentId,
                            "content",
                            "reply content"),
                        actorHeaders),
                    Map.class)),
            "id");

    Map<?, ?> drain = eventOutboxService.publishPending();
    assertThat(number(drain, "published")).isGreaterThanOrEqualTo(1);

    Map<?, ?> ownerPage =
        data(
            restTemplate.exchange(
                "/api/v1/notifications?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(ownerHeaders),
                Map.class));
    assertThat(number(ownerPage, "unread_count")).isEqualTo(1);
    List<?> list = (List<?>) ownerPage.get("list");
    assertThat(list).hasSize(1);
    Map<?, ?> notification = (Map<?, ?>) list.getFirst();
    assertThat(notification.get("type")).isEqualTo("comment_reply");
    assertThat(notification.get("link")).isEqualTo("/posts/" + targetKey + "?commentId=" + replyId);
    assertThat(number(notification, "target_comment_id")).isEqualTo(replyId);

    Map<?, ?> actorPage =
        data(
            restTemplate.exchange(
                "/api/v1/notifications?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(actorHeaders),
                Map.class));
    assertThat(number(actorPage, "unread_count")).isEqualTo(0);
    assertThat((List<?>) actorPage.get("list")).isEmpty();

    ResponseEntity<Map> markRead =
        restTemplate.exchange(
            "/api/v1/notifications/" + notification.get("id") + "/read",
            HttpMethod.PUT,
            new HttpEntity<>(ownerHeaders),
            Map.class);
    assertThat(markRead.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<?, ?> unread =
        data(
            restTemplate.exchange(
                "/api/v1/notifications/unread-count",
                HttpMethod.GET,
                new HttpEntity<>(ownerHeaders),
                Map.class));
    assertThat(number(unread, "unread_count")).isEqualTo(0);
  }

  @Test
  void selfReplyDoesNotCreateNotification() {
    String targetKey = "p3-notification-self-" + System.nanoTime();
    HttpHeaders headers = registerAndAuth("p3-notify-self-" + System.nanoTime() + "@example.com", "Notify Self");

    long parentId =
        number(
            data(
                restTemplate.exchange(
                    "/api/v1/comments",
                    HttpMethod.POST,
                    new HttpEntity<>(
                        Map.of("target_type", "article", "target_key", targetKey, "content", "self root"),
                        headers),
                    Map.class)),
            "id");
    restTemplate.exchange(
        "/api/v1/comments",
        HttpMethod.POST,
        new HttpEntity<>(
            Map.of(
                "target_type",
                "article",
                "target_key",
                targetKey,
                "parent_id",
                parentId,
                "content",
                "self reply"),
            headers),
        Map.class);

    eventOutboxService.publishPending();

    Map<?, ?> page =
        data(
            restTemplate.exchange(
                "/api/v1/notifications?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    assertThat(number(page, "unread_count")).isEqualTo(0);
    assertThat((List<?>) page.get("list")).isEmpty();
  }

  private HttpHeaders registerAndAuth(String email, String nickname) {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", email, "nickname", nickname, "password", "reader123456"),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(data(response).get("access_token").toString());
    return headers;
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(0);
    return (Map<?, ?>) response.getBody().get("data");
  }

  private long number(Map<?, ?> map, String key) {
    Object value = map.get(key);
    assertThat(value).isInstanceOf(Number.class);
    return ((Number) value).longValue();
  }
}
