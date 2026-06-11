package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Phase4InteractionApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void settingsCanBeReadPubliclyAndUpdatedByAdmin() {
    ResponseEntity<Map> publicBefore = restTemplate.getForEntity("/api/v1/settings/basic", Map.class);
    assertThat(publicBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Map<?, ?>) data(publicBefore)).get("site_name")).isNotNull();

    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/admin/settings/basic",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("site_name", "ZBlog Test Site"), headers),
            Map.class);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> publicAfter = restTemplate.getForEntity("/api/v1/settings/basic", Map.class);
    assertThat(((Map<?, ?>) data(publicAfter)).get("site_name")).isEqualTo("ZBlog Test Site");
  }

  @Test
  void frontConfigAndMenusProvideV2Baseline() {
    ResponseEntity<Map> configResponse = restTemplate.getForEntity("/api/v1/front/config", Map.class);
    ResponseEntity<Map> menusResponse = restTemplate.getForEntity("/api/v1/front/menus", Map.class);

    assertThat(configResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(menusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<String, Object> config = castMap(data(configResponse));
    Map<String, Object> identity = castMap(config.get("identity"));
    Map<String, Object> home = castMap(config.get("home"));
    Map<String, Object> about = castMap(config.get("about"));
    Map<String, Object> guestbook = castMap(config.get("guestbook"));
    Map<String, Object> footer = castMap(config.get("footer"));
    Map<String, Object> menus = castMap(data(menusResponse));

    assertThat(identity)
        .containsKeys(
            "siteTitle",
            "ownerDisplayName",
            "email",
            "primaryImageUrl",
            "faviconUrl",
            "icpRecord",
            "policeRecord");
    assertThat(home)
        .containsKeys(
            "heroEyebrow", "heroTitle", "heroMeta", "heroCtaLabel", "heroCtaTarget");
    assertThat(about)
        .containsKeys(
            "introText", "statusItems", "skillItems", "timelineItems", "bottomQuote");
    assertThat(guestbook).containsKeys("introText", "backgroundImage");
    assertThat(footer)
        .containsKeys("description", "copyrightText", "slogan", "socialLinks");

    assertThat(menus).containsKeys("header", "footer");
    assertThat((List<?>) menus.get("header")).isNotEmpty();
    assertThat((List<?>) menus.get("footer")).isNotEmpty();
    assertThat((List<?>) menus.get("header"))
        .anySatisfy(
            menu -> {
              Map<?, ?> item = (Map<?, ?>) menu;
              assertThat(item.get("type")).isEqualTo("header_navigation");
              assertThat(item.get("title")).isEqualTo("写作");
              assertThat((List<?>) item.get("children")).isNotEmpty();
            });
    assertThat((List<?>) menus.get("footer"))
        .allSatisfy(
            menu -> {
              Map<?, ?> item = (Map<?, ?>) menu;
              assertThat(item.get("type")).isEqualTo("footer_navigation");
            });
  }

  @Test
  void adminMenusCanBeCreatedUpdatedAndDeleted() {
    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> createResponse =
        restTemplate.exchange(
            "/api/v1/admin/menus",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "type", "header_navigation",
                    "title", "测试菜单",
                    "url", "/test-menu",
                    "icon", "ri-test-tube-line",
                    "sort", 9,
                    "is_enabled", true),
                headers),
            Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Number parentId = (Number) ((Map<?, ?>) data(createResponse)).get("id");

    ResponseEntity<Map> childResponse =
        restTemplate.exchange(
            "/api/v1/admin/menus",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "type", "header_navigation",
                    "parent_id", parentId,
                    "title", "测试子菜单",
                    "url", "/test-child-menu",
                    "icon", "",
                    "sort", 10,
                    "is_enabled", true),
                headers),
            Map.class);
    assertThat(childResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> updateResponse =
        restTemplate.exchange(
            "/api/v1/admin/menus/" + parentId,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("title", "测试菜单更新"), headers),
            Map.class);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Map<?, ?>) data(updateResponse)).get("title")).isEqualTo("测试菜单更新");

    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/menus?type=header_navigation",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    List<?> menus = (List<?>) data(listResponse);
    assertThat(menus)
        .anySatisfy(
            menu -> {
              Map<?, ?> item = (Map<?, ?>) menu;
              assertThat(item.get("id")).isEqualTo(parentId);
              assertThat((List<?>) item.get("children")).hasSize(1);
            });

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/menus/" + parentId,
            HttpMethod.DELETE,
            new HttpEntity<>(Map.of("children_action", "delete"), headers),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void friendsCanBeManagedAndAppliedPublicly() {
    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> typeResponse =
        restTemplate.exchange(
            "/api/v1/admin/friends/types",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("name", "Friend Links", "sort", 1, "is_visible", true), headers),
            Map.class);
    assertThat(typeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Number typeId = (Number) ((Map<?, ?>) data(typeResponse)).get("id");

    ResponseEntity<Map> friendResponse =
        restTemplate.exchange(
            "/api/v1/admin/friends",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "name", "Example Blog",
                    "url", "https://example.com",
                    "description", "A friendly example",
                    "avatar", "https://example.com/avatar.png",
                    "sort", 1,
                    "type_id", typeId),
                headers),
            Map.class);
    assertThat(friendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> groupedResponse = restTemplate.getForEntity("/api/v1/friends", Map.class);
    Map<?, ?> grouped = (Map<?, ?>) data(groupedResponse);
    assertThat(((Number) grouped.get("total_friends")).intValue()).isGreaterThanOrEqualTo(1);
    assertThat((List<?>) grouped.get("groups")).isNotEmpty();

    ResponseEntity<Map> applyResponse =
        restTemplate.postForEntity(
            "/api/v1/friends/apply",
            Map.of(
                "name", "Pending Blog",
                "url", "https://pending.example.com",
                "description", "Pending approval",
                "avatar", "https://pending.example.com/avatar.png"),
            Map.class);
    assertThat(applyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Map<?, ?>) data(applyResponse)).get("is_pending")).isEqualTo(true);
  }

  @Test
  void commentsCanBeSubmittedListedHiddenAndDeleted() {
    ResponseEntity<Map> createResponse =
        restTemplate.postForEntity(
            "/api/v1/comments",
            Map.of(
                "target_type", "article",
                "target_key", "hello-zblog",
                "content", "Phase 4 comment",
                "nickname", "Guest",
                "email", "guest@example.com",
                "website", "https://guest.example.com"),
            Map.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Number commentId = (Number) ((Map<?, ?>) data(createResponse)).get("id");

    ResponseEntity<Map> publicList =
        restTemplate.getForEntity(
            "/api/v1/comments?target_type=article&target_key=hello-zblog", Map.class);
    assertThat((List<?>) ((Map<?, ?>) data(publicList)).get("list")).isNotEmpty();

    HttpHeaders headers = authenticatedHeaders();
    ResponseEntity<Map> toggleResponse =
        restTemplate.exchange(
            "/api/v1/admin/comments/" + commentId + "/toggle-status",
            HttpMethod.PUT,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(toggleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> hiddenList =
        restTemplate.getForEntity(
            "/api/v1/comments?target_type=article&target_key=hello-zblog", Map.class);
    assertThat((List<?>) ((Map<?, ?>) data(hiddenList)).get("list")).isEmpty();

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/comments/" + commentId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void publicFeedbackAttachmentCanBeUploadedWithoutLogin() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file",
        new ByteArrayResource("feedback image".getBytes()) {
          @Override
          public String getFilename() {
            return "feedback.png";
          }
        });
    body.add("type", "反馈投诉");

    ResponseEntity<Map> uploadResponse =
        restTemplate.exchange("/api/v1/upload", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> upload = (Map<?, ?>) data(uploadResponse);
    assertThat(upload.get("file_url")).asString().contains("/uploads/");
  }

  @Test
  void filesCanBeUploadedListedAndDeleted() {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file",
        new ByteArrayResource("hello file".getBytes()) {
          @Override
          public String getFilename() {
            return "hello.txt";
          }
        });
    body.add("type", "document");

    ResponseEntity<Map> uploadResponse =
        restTemplate.exchange(
            "/api/v1/admin/files", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> upload = (Map<?, ?>) data(uploadResponse);
    assertThat(upload.get("file_url")).asString().contains("/uploads/");

    HttpHeaders authHeaders = authenticatedHeaders();
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/files", HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class);
    Map<?, ?> page = (Map<?, ?>) data(listResponse);
    assertThat((List<?>) page.get("list")).isNotEmpty();
    Number fileId = (Number) ((Map<?, ?>) ((List<?>) page.get("list")).getFirst()).get("id");

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/files/" + fileId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login",
            Map.of("username", "admin", "password", "admin123456"),
            Map.class);
    String accessToken = (String) ((Map<?, ?>) data(loginResponse)).get("access_token");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private Object data(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }
}
