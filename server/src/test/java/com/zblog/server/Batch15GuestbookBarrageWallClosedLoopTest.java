package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.LocalDate;
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
import org.springframework.web.util.UriComponentsBuilder;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch15GuestbookBarrageWallClosedLoopTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void publicSubmitPersistsMessageAndShowsImmediately() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/guestbook/messages",
            Map.of(
                "nickname", "Batch15 Visitor",
                "email", "batch15@example.com",
                "content", "Batch15 public submit persists"),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> created = (Map<?, ?>) data(response);
    assertThat(created.get("id")).isNotNull();
    assertThat(created.get("status")).isEqualTo("pending");
    assertThat(created.get("message")).asString().isNotBlank();
    // Pending message does NOT appear in public list (requires approval)
    assertThat(list(publicMessages())).noneSatisfy(row -> assertPublicMessage(row, created.get("id"), "Batch15 Visitor"));
    // But it DOES appear in the admin list

    Map<?, ?> adminPage = adminGet("/api/v1/admin/guestbook/messages?keyword=Batch15%20public%20submit", authenticatedHeaders());
    assertThat(list(adminPage)).anySatisfy(row -> assertThat(((Map<?, ?>) row).get("id")).isEqualTo(created.get("id")));
  }

  @Test
  void rejectedAndHiddenMessagesDoNotAppearInPublicList() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> rejected = submit("Batch15 Rejected", "Batch15 rejected content");
    Map<?, ?> hidden = submit("Batch15 Hidden", "Batch15 hidden content");

    updateStatus(rejected.get("id"), "rejected", headers);
    updateStatus(hidden.get("id"), "hidden", headers);

    Map<?, ?> publicPage = publicMessages();
    assertThat(list(publicPage))
        .noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("id")).isEqualTo(rejected.get("id")))
        .noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("id")).isEqualTo(hidden.get("id")));
  }

  @Test
  void adminApproveMakesMessageVisibleAndRefreshSafe() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> created = submit("Batch15 Approved", "Batch15 approved content survives reread");

    updateStatus(created.get("id"), "approved", headers);

    Map<?, ?> firstRead = publicMessages();
    Map<?, ?> secondRead = publicMessages();
    assertThat(list(firstRead)).anySatisfy(row -> assertPublicMessage(row, created.get("id"), "Batch15 Approved"));
    assertThat(list(secondRead)).anySatisfy(row -> assertPublicMessage(row, created.get("id"), "Batch15 Approved"));
  }

  @Test
  void publicApiDoesNotExposePrivateOrAdminOnlyFields() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> created = submit("Batch15 Privacy", "Batch15 public privacy content");
    updateStatus(created.get("id"), "approved", headers);

    Map<?, ?> publicRow = findPublic(created.get("id"));

    assertThat(publicRow.containsKey("id")).isTrue();
    assertThat(publicRow.containsKey("nickname")).isTrue();
    assertThat(publicRow.containsKey("content")).isTrue();
    assertThat(publicRow.containsKey("pinned")).isTrue();
    assertThat(publicRow.containsKey("created_at")).isTrue();
    assertThat(publicRow.containsKey("email")).isFalse();
    assertThat(publicRow.containsKey("ip")).isFalse();
    assertThat(publicRow.containsKey("user_agent")).isFalse();
    assertThat(publicRow.containsKey("admin_note")).isFalse();
    assertThat(publicRow.containsKey("browser")).isFalse();
    assertThat(publicRow.containsKey("os")).isFalse();
    assertThat(publicRow.containsKey("location")).isFalse();
    assertThat(publicRow.containsKey("status")).isFalse();
  }

  @Test
  void adminFiltersByKeywordStatusPinnedAndDateRange() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> pinned = submit("Batch15 Filter Pinned", "Batch15 filter alpha keyword");
    Map<?, ?> rejected = submit("Batch15 Filter Rejected", "Batch15 filter beta keyword");

    updateStatus(pinned.get("id"), "approved", headers);
    updatePin(pinned.get("id"), true, headers);
    updateStatus(rejected.get("id"), "rejected", headers);

    assertAdminIds(adminGet("/api/v1/admin/guestbook/messages?keyword=alpha", headers), pinned.get("id"));
    assertAdminIds(adminGet("/api/v1/admin/guestbook/messages?status=approved&keyword=Batch15%20Filter", headers), pinned.get("id"));
    assertAdminIds(adminGet("/api/v1/admin/guestbook/messages?pinned=true&keyword=Batch15%20Filter", headers), pinned.get("id"));
    assertAdminIds(adminGet("/api/v1/admin/guestbook/messages?status=rejected&keyword=Batch15%20Filter", headers), rejected.get("id"));

    String today = LocalDate.now().toString();
    assertAdminIds(
        adminGet(
            "/api/v1/admin/guestbook/messages?start_time="
                + today
                + "&end_time="
                + today
                + "&keyword=Batch15%20Filter",
            headers),
        pinned.get("id"),
        rejected.get("id"));
  }

  @Test
  void pinAndUnpinChangePublicPinnedFieldAndOrdering() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> normal = submit("Batch15 Normal", "Batch15 normal ordering");
    Map<?, ?> pinned = submit("Batch15 Pinned", "Batch15 pinned ordering");
    updateStatus(normal.get("id"), "approved", headers);
    updateStatus(pinned.get("id"), "approved", headers);

    updatePin(pinned.get("id"), true, headers);

    List<?> afterPin = list(publicMessages());
    assertThat(((Map<?, ?>) afterPin.getFirst()).get("id")).isEqualTo(pinned.get("id"));
    assertThat(((Map<?, ?>) afterPin.getFirst()).get("pinned")).isEqualTo(true);

    updatePin(pinned.get("id"), false, headers);

    Map<?, ?> afterUnpin = findPublic(pinned.get("id"));
    assertThat(afterUnpin.get("pinned")).isEqualTo(false);
  }

  @Test
  void deleteRemovesMessageFromPublicAndAdminNormalLists() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> created = submit("Batch15 Delete", "Batch15 delete content");
    updateStatus(created.get("id"), "approved", headers);

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/guestbook/messages/" + created.get("id"),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    data(deleteResponse);

    assertThat(list(publicMessages())).noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("id")).isEqualTo(created.get("id")));
    assertThat(list(adminGet("/api/v1/admin/guestbook/messages?keyword=Batch15%20Delete", headers)))
        .noneSatisfy(row -> assertThat(((Map<?, ?>) row).get("id")).isEqualTo(created.get("id")));
  }

  private Map<?, ?> submit(String nickname, String content) {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/guestbook/messages",
            Map.of("nickname", nickname, "email", nickname.replace(' ', '.').toLowerCase() + "@example.com", "content", content),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (Map<?, ?>) data(response);
  }

  private void updateStatus(Object id, String status, HttpHeaders headers) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/guestbook/messages/" + id + "/status",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("status", status, "admin_note", "Batch15 moderation note"), headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    data(response);
  }

  private void updatePin(Object id, boolean pinned, HttpHeaders headers) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/guestbook/messages/" + id + "/pin",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("pinned", pinned), headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    data(response);
  }

  private Map<?, ?> publicMessages() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/guestbook/messages?page=1&page_size=100", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (Map<?, ?>) data(response);
  }

  private Map<?, ?> findPublic(Object id) {
    return list(publicMessages()).stream()
        .map(Map.class::cast)
        .filter(row -> row.get("id").equals(id))
        .findFirst()
        .orElseThrow();
  }

  private void assertPublicMessage(Object row, Object id, String nickname) {
    Map<?, ?> item = (Map<?, ?>) row;
    assertThat(item.get("id")).isEqualTo(id);
    assertThat(item.get("nickname")).isEqualTo(nickname);
  }

  private Map<?, ?> adminGet(String path, HttpHeaders headers) {
    ResponseEntity<Map> response = restTemplate.exchange(uri(path), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (Map<?, ?>) data(response);
  }

  private void assertAdminIds(Map<?, ?> page, Object... expectedIds) {
    List<?> actualIds = list(page).stream().map(row -> ((Map<?, ?>) row).get("id")).toList();
    assertThat(actualIds).hasSize(expectedIds.length);
    for (Object expectedId : expectedIds) {
      assertThat(actualIds.stream().anyMatch(actualId -> actualId.equals(expectedId))).isTrue();
    }
  }

  private List<?> list(Map<?, ?> page) {
    return (List<?>) page.get("list");
  }

  private URI uri(String path) {
    return UriComponentsBuilder.fromUriString(path).build(true).toUri();
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
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
