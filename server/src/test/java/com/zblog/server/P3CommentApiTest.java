package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.web.client.ResourceAccessException;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class P3CommentApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void commentsRequireLoginAndUseCurrentUserProfile() {
    String targetKey = "p3-comments-" + System.nanoTime();
    assertThatThrownBy(
            () ->
                restTemplate.postForEntity(
                    "/api/v1/comments",
                    Map.of("target_type", "article", "target_key", targetKey, "content", "hello"),
                    Map.class))
        .isInstanceOf(ResourceAccessException.class);

    HttpHeaders headers = registerAndAuth("p3-comment-" + System.nanoTime() + "@example.com", "Comment Reader");
    ResponseEntity<Map> created =
        restTemplate.exchange(
            "/api/v1/comments",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("target_type", "article", "target_key", targetKey, "content", "第一条评论"),
                headers),
            Map.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> comment = data(created);
    assertThat(comment.get("content")).isEqualTo("第一条评论");
    assertThat(((Map<?, ?>) comment.get("user")).get("nickname")).isEqualTo("Comment Reader");
  }

  @Test
  void repliesAreNestedAndOnlyOwnerCanDeletePublicComment() {
    String targetKey = "p3-comment-reply-" + System.nanoTime();
    HttpHeaders ownerHeaders = registerAndAuth("p3-owner-" + System.nanoTime() + "@example.com", "Owner Reader");
    HttpHeaders otherHeaders = registerAndAuth("p3-other-" + System.nanoTime() + "@example.com", "Other Reader");

    long parentId =
        ((Number)
                data(
                        restTemplate.exchange(
                            "/api/v1/comments",
                            HttpMethod.POST,
                            new HttpEntity<>(
                                Map.of("target_type", "article", "target_key", targetKey, "content", "root"),
                                ownerHeaders),
                            Map.class))
                    .get("id"))
            .longValue();
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
                "reply"),
            ownerHeaders),
        Map.class);

    ResponseEntity<Map> listed =
        restTemplate.getForEntity(
            "/api/v1/comments?target_type=article&target_key=" + targetKey + "&page=1&page_size=20",
            Map.class);
    Map<?, ?> root = ((List<Map<?, ?>>) data(listed).get("list")).getFirst();
    assertThat(root.get("id").toString()).isEqualTo(String.valueOf(parentId));
    assertThat((List<?>) root.get("replies")).hasSize(1);

    ResponseEntity<Map> forbidden =
        restTemplate.exchange(
            "/api/v1/comments/" + parentId, HttpMethod.DELETE, new HttpEntity<>(otherHeaders), Map.class);
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> deleted =
        restTemplate.exchange(
            "/api/v1/comments/" + parentId, HttpMethod.DELETE, new HttpEntity<>(ownerHeaders), Map.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> listedAfterDelete =
        restTemplate.getForEntity(
            "/api/v1/comments?target_type=article&target_key=" + targetKey + "&page=1&page_size=20",
            Map.class);
    Map<?, ?> deletedRoot = ((List<Map<?, ?>>) data(listedAfterDelete).get("list")).getFirst();
    assertThat(deletedRoot.get("content")).isEqualTo("评论已删除");
    assertThat(deletedRoot.get("is_deleted")).isEqualTo(true);
    assertThat(((Map<?, ?>) deletedRoot.get("user")).get("nickname")).isEqualTo("已注销用户");
    assertThat((List<?>) deletedRoot.get("replies")).hasSize(1);
  }

  @Test
  void repliesArePagedAndCommentCanBeLocated() {
    String targetKey = "p3-comment-paging-" + System.nanoTime();
    HttpHeaders ownerHeaders = registerAndAuth("p3-page-owner-" + System.nanoTime() + "@example.com", "Page Owner");
    HttpHeaders actorHeaders = registerAndAuth("p3-page-actor-" + System.nanoTime() + "@example.com", "Page Actor");

    long rootId =
        ((Number)
                data(
                        restTemplate.exchange(
                            "/api/v1/comments",
                            HttpMethod.POST,
                            new HttpEntity<>(
                                Map.of("target_type", "article", "target_key", targetKey, "content", "root"),
                                ownerHeaders),
                            Map.class))
                    .get("id"))
            .longValue();
    long lastReplyId = 0;
    for (int i = 1; i <= 12; i++) {
      lastReplyId =
          ((Number)
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
                                      rootId,
                                      "content",
                                      "reply-" + i),
                                  actorHeaders),
                              Map.class))
                      .get("id"))
              .longValue();
    }

    Map<?, ?> listed =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments?target_type=article&target_key=" + targetKey + "&page=1&page_size=10&reply_page_size=10",
                Map.class));
    Map<?, ?> root = ((List<Map<?, ?>>) listed.get("list")).getFirst();
    assertThat(root.get("id").toString()).isEqualTo(String.valueOf(rootId));
    assertThat(((Number) root.get("reply_total")).longValue()).isEqualTo(12);
    assertThat((List<?>) root.get("replies")).hasSize(10);

    Map<?, ?> replyPage =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments/" + rootId + "/replies?page=2&page_size=10",
                Map.class));
    assertThat((List<?>) replyPage.get("list")).hasSize(2);

    Map<?, ?> location =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments/locate?target_type=article&target_key="
                    + targetKey
                    + "&comment_id="
                    + lastReplyId
                    + "&page_size=10&reply_page_size=10",
                Map.class));
    assertThat(((Number) location.get("root_id")).longValue()).isEqualTo(rootId);
    assertThat(((Number) location.get("root_page")).longValue()).isEqualTo(1);
    assertThat(((Number) location.get("reply_page")).longValue()).isEqualTo(2);
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
}
