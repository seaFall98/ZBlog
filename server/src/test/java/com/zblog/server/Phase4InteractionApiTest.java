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
    assertThat(grouped.get("total_friends")).isEqualTo(1);
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
}
