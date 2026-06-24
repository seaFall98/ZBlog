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
class P3CommentApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void commentsRequireLoginAndUseCurrentUserProfile() {
    String targetKey = "p3-comments-" + System.nanoTime();
    ResponseEntity<Map> anonymous =
        restTemplate.postForEntity(
            "/api/v1/comments",
            Map.of("target_type", "article", "target_key", targetKey, "content", "hello"),
            Map.class);
    assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

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

  @Test
  void rootsRemainVisibleWhenRootIdIsBackfilledToSelf() {
    String targetKey = "p3-comment-root-backfill-" + System.nanoTime();
    HttpHeaders headers = registerAndAuth("p3-root-backfill-" + System.nanoTime() + "@example.com", "Root Backfill");

    long rootId =
        ((Number)
                data(
                        restTemplate.exchange(
                            "/api/v1/comments",
                            HttpMethod.POST,
                            new HttpEntity<>(
                                Map.of("target_type", "article", "target_key", targetKey, "content", "root"),
                                headers),
                            Map.class))
                    .get("id"))
            .longValue();
    restTemplate.exchange(
        "/api/v1/comments",
        HttpMethod.POST,
        new HttpEntity<>(
            Map.of("target_type", "article", "target_key", targetKey, "parent_id", rootId, "content", "reply"),
            headers),
        Map.class);

    // Simulate V28 applying to existing data: root comments may have root_id = id.
    jdbcTemplate().update("update comments set root_id = id where id = ?", rootId);

    Map<?, ?> listed =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments?target_type=article&target_key=" + targetKey + "&page=1&page_size=10",
                Map.class));
    assertThat((List<?>) listed.get("list")).hasSize(1);
    Map<?, ?> root = ((List<Map<?, ?>>) listed.get("list")).getFirst();
    assertThat(root.get("id").toString()).isEqualTo(String.valueOf(rootId));
    assertThat(((Number) root.get("reply_total")).longValue()).isEqualTo(1);
  }

  @Test
  void adminReplyUsesAuthenticatedAdminProfile() {
    String targetKey = "p3-comment-admin-reply-" + System.nanoTime();
    HttpHeaders ownerHeaders = registerAndAuth("p3-admin-target-" + System.nanoTime() + "@example.com", "Admin Target");
    HttpHeaders adminHeaders = adminHeaders();

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

    ResponseEntity<Map> created =
        restTemplate.exchange(
            "/api/v1/admin/comments",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("target_type", "article", "target_key", targetKey, "parent_id", parentId, "content", "admin reply"),
                adminHeaders),
            Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<?, ?> listed =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments?target_type=article&target_key=" + targetKey + "&page=1&page_size=10",
                Map.class));
    Map<?, ?> root = ((List<Map<?, ?>>) listed.get("list")).getFirst();
    Map<?, ?> reply = (Map<?, ?>) ((List<?>) root.get("replies")).getFirst();
    assertThat(((Map<?, ?>) reply.get("user")).get("nickname")).isEqualTo("admin");
    assertThat(((Number) ((Map<?, ?>) reply.get("user")).get("id")).longValue()).isGreaterThan(0);
  }

  @Test
  void commentLikesSortingPinAndReplyPreviewFollowBatch8Contract() {
    String targetKey = "p3-comment-batch8-interaction-" + System.nanoTime();
    HttpHeaders ownerHeaders = registerAndAuth("p3-b8-owner-" + System.nanoTime() + "@example.com", "Batch8 Owner");
    HttpHeaders actorHeaders = registerAndAuth("p3-b8-actor-" + System.nanoTime() + "@example.com", "Batch8 Actor");
    HttpHeaders adminHeaders = adminHeaders();

    long olderRoot = createComment(targetKey, "older root", ownerHeaders);
    long newerRoot = createComment(targetKey, "newer root", ownerHeaders);
    for (int i = 1; i <= 4; i++) {
      createReply(targetKey, olderRoot, "reply-" + i, actorHeaders);
    }

    ResponseEntity<Map> anonymousLike =
        restTemplate.exchange("/api/v1/comments/" + olderRoot + "/like", HttpMethod.POST, HttpEntity.EMPTY, Map.class);
    assertThat(anonymousLike.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    Map<?, ?> liked =
        data(
            restTemplate.exchange(
                "/api/v1/comments/" + olderRoot + "/like",
                HttpMethod.POST,
                new HttpEntity<>(actorHeaders),
                Map.class));
    assertThat(liked.get("liked_by_me")).isEqualTo(true);
    assertThat(((Number) liked.get("like_count")).longValue()).isEqualTo(1);

    Map<?, ?> hotPage =
        data(
            restTemplate.exchange(
                "/api/v1/comments?target_type=article&target_key=" + targetKey + "&sort=hot&page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(actorHeaders),
                Map.class));
    Map<?, ?> firstHot = ((List<Map<?, ?>>) hotPage.get("list")).getFirst();
    assertThat(((Number) firstHot.get("id")).longValue()).isEqualTo(olderRoot);
    assertThat(firstHot.get("liked_by_me")).isEqualTo(true);
    assertThat((List<?>) firstHot.get("replies")).hasSize(3);
    assertThat(((Number) firstHot.get("reply_total")).longValue()).isEqualTo(4);

    Map<?, ?> latestPage =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments?target_type=article&target_key=" + targetKey + "&sort=latest&page=1&page_size=10",
                Map.class));
    Map<?, ?> firstLatest = ((List<Map<?, ?>>) latestPage.get("list")).getFirst();
    assertThat(((Number) firstLatest.get("id")).longValue()).isEqualTo(newerRoot);

    Map<?, ?> pinned =
        data(
            restTemplate.exchange(
                "/api/v1/admin/comments/" + newerRoot + "/pin",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("pinned", true), adminHeaders),
                Map.class));
    assertThat(pinned.get("pinned")).isEqualTo(true);

    Map<?, ?> hotAfterPin =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments?target_type=article&target_key=" + targetKey + "&sort=hot&page=1&page_size=10",
                Map.class));
    Map<?, ?> firstPinned = ((List<Map<?, ?>>) hotAfterPin.get("list")).getFirst();
    assertThat(((Number) firstPinned.get("id")).longValue()).isEqualTo(newerRoot);
    assertThat(firstPinned.get("pinned")).isEqualTo(true);

    Map<?, ?> unliked =
        data(
            restTemplate.exchange(
                "/api/v1/comments/" + olderRoot + "/like",
                HttpMethod.POST,
                new HttpEntity<>(actorHeaders),
                Map.class));
    assertThat(unliked.get("liked_by_me")).isEqualTo(false);
    assertThat(((Number) unliked.get("like_count")).longValue()).isEqualTo(0);
  }

  @Test
  void uploadedCommentImageMustBeOwnedRecentAndIsBoundOnCommentCreate() {
    String targetKey = "p3-comment-batch8-image-" + System.nanoTime();
    HttpHeaders adminHeaders = adminHeaders();

    Map<?, ?> upload = data(uploadCommentImage(adminHeaders));
    String fileUrl = upload.get("file_url").toString();
    long fileId = ((Number) upload.get("id")).longValue();

    ResponseEntity<Map> created =
        restTemplate.exchange(
            "/api/v1/comments",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("target_type", "article", "target_key", targetKey, "content", "image ![图片](" + fileUrl + ")"),
                adminHeaders),
            Map.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
    long commentId = ((Number) data(created).get("id")).longValue();
    assertThat(
            jdbcTemplate.queryForObject(
                "select bound_comment_id from files where id = ?", Long.class, fileId))
        .isEqualTo(commentId);
    assertThat(
            jdbcTemplate.queryForObject(
                "select storage_provider from files where id = ?", String.class, fileId))
        .isEqualTo("local");

    Map<?, ?> secondUpload = data(uploadCommentImage(adminHeaders));
    String secondUrl = secondUpload.get("file_url").toString();
    ResponseEntity<Map> invalid =
        restTemplate.exchange(
            "/api/v1/comments",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "target_type",
                    "article",
                    "target_key",
                    targetKey,
                    "content",
                    "two ![图片](" + fileUrl + ") ![图片](" + secondUrl + ")"),
                adminHeaders),
            Map.class);
    assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

  private HttpHeaders adminHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(data(response).get("access_token").toString());
    return headers;
  }

  private long createComment(String targetKey, String content, HttpHeaders headers) {
    return ((Number)
            data(
                    restTemplate.exchange(
                        "/api/v1/comments",
                        HttpMethod.POST,
                        new HttpEntity<>(Map.of("target_type", "article", "target_key", targetKey, "content", content), headers),
                        Map.class))
                .get("id"))
        .longValue();
  }

  private long createReply(String targetKey, long parentId, String content, HttpHeaders headers) {
    return ((Number)
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
                                content),
                            headers),
                        Map.class))
                .get("id"))
        .longValue();
  }

  private ResponseEntity<Map> uploadCommentImage(HttpHeaders headers) {
    HttpHeaders uploadHeaders = new HttpHeaders();
    uploadHeaders.putAll(headers);
    uploadHeaders.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
    org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
    body.add("file", resource("comment.png", pngBytes(), org.springframework.http.MediaType.IMAGE_PNG));
    body.add("type", "评论贴图");
    return restTemplate.exchange("/api/v1/upload", HttpMethod.POST, new HttpEntity<>(body, uploadHeaders), Map.class);
  }

  private HttpEntity<org.springframework.core.io.ByteArrayResource> resource(
      String filename, byte[] bytes, org.springframework.http.MediaType mediaType) {
    org.springframework.core.io.ByteArrayResource body =
        new org.springframework.core.io.ByteArrayResource(bytes) {
          @Override
          public String getFilename() {
            return filename;
          }

          @Override
          public long contentLength() {
            return bytes.length;
          }
        };
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return new HttpEntity<>(body, headers);
  }

  private byte[] pngBytes() {
    return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, 3};
  }

  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

  private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate() {
    return jdbcTemplate;
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(0);
    return (Map<?, ?>) response.getBody().get("data");
  }
}
