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
class BackendTruthDataBatchTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void collectPageviewChangesStatsTrendAndVisitLog() {
    HttpHeaders adminHeaders = authenticatedHeaders();
    long beforePageViews = number(data(restTemplate.getForEntity("/api/v1/stats/site", Map.class)), "total_page_views");

    ResponseEntity<Map> collectResponse =
        restTemplate.postForEntity(
            "/api/v1/collect",
            Map.of(
                "type", "pageview",
                "url", "/posts/backend-truth-data",
                "hostname", "localhost",
                "referrer", "",
                "language", "zh-CN",
                "screen", "1920x1080",
                "title", "Backend Truth Data",
                "timestamp", System.currentTimeMillis()),
            Map.class);
    assertThat(collectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<?, ?> site = data(restTemplate.getForEntity("/api/v1/stats/site", Map.class));
    assertThat(number(site, "total_page_views")).isEqualTo(beforePageViews + 1);
    assertThat(number(site, "total_visitors")).isGreaterThanOrEqualTo(1);

    Map<?, ?> dashboard =
        data(
            restTemplate.exchange(
                "/api/v1/admin/stats/dashboard", HttpMethod.GET, new HttpEntity<>(adminHeaders), Map.class));
    assertThat(number(dashboard, "total_views")).isGreaterThanOrEqualTo(beforePageViews + 1);
    assertThat(number(dashboard, "today_views")).isGreaterThanOrEqualTo(1);

    List<?> trend =
        (List<?>)
            responseData(
                restTemplate.exchange(
                    "/api/v1/admin/stats/trend?type=daily",
                    HttpMethod.GET,
                    new HttpEntity<>(adminHeaders),
                    Map.class));
    assertThat(trend).anySatisfy(row -> assertThat(number((Map<?, ?>) row, "pv_count")).isGreaterThanOrEqualTo(1));

    Map<?, ?> visits =
        data(
            restTemplate.exchange(
                "/api/v1/admin/stats/visits?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                Map.class));
    assertThat((List<?>) visits.get("list"))
        .anySatisfy(row -> assertThat(((Map<?, ?>) row).get("url")).isEqualTo("/posts/backend-truth-data"));
  }

  @Test
  void feedbackCreatesNotificationAndReadOperationsUpdateUnreadCount() {
    ResponseEntity<Map> feedbackResponse =
        restTemplate.postForEntity(
            "/api/v1/feedback",
            Map.of(
                "reportUrl", "/posts/backend-truth-data",
                "reportType", "suggestion",
                "email", "notify@example.com",
                "description", "Create a notification"),
            Map.class);
    assertThat(feedbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> notificationPage =
        data(
            restTemplate.exchange(
                "/api/v1/admin/notifications?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    assertThat(number(notificationPage, "unread_count")).isGreaterThanOrEqualTo(1);
    List<?> list = (List<?>) notificationPage.get("list");
    assertThat(list).isNotEmpty();
    Number id = (Number) ((Map<?, ?>) list.getFirst()).get("id");

    ResponseEntity<Map> readResponse =
        restTemplate.exchange(
            "/api/v1/admin/notifications/" + id + "/read",
            HttpMethod.PUT,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<?, ?> afterRead =
        data(
            restTemplate.exchange(
                "/api/v1/admin/notifications?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    assertThat(number(afterRead, "unread_count")).isLessThan(number(notificationPage, "unread_count"));

    restTemplate.exchange(
        "/api/v1/admin/notifications/read-all", HttpMethod.PUT, new HttpEntity<>(headers), Map.class);
    Map<?, ?> afterReadAll =
        data(
            restTemplate.exchange(
                "/api/v1/notifications?page=1&page_size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    assertThat(number(afterReadAll, "unread_count")).isEqualTo(0);
  }

  @Test
  void systemInfoReturnsRealDbRuntimeAndHonestUnsupportedFields() {
    HttpHeaders headers = authenticatedHeaders();

    Map<?, ?> statics =
        data(
            restTemplate.exchange(
                "/api/v1/admin/system/static", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
    assertThat(number(statics, "cpu_core")).isGreaterThan(0);
    assertThat(number(statics, "memory_total")).isGreaterThan(0);
    assertThat(number(statics, "disk_total")).isGreaterThan(0);
    assertThat(number(statics, "db_tables")).isGreaterThan(0);
    assertThat(statics.get("email_status")).isEqualTo("disabled");
    assertThat(statics.get("feishu_status")).isEqualTo("disabled");

    Map<?, ?> dynamic =
        data(
            restTemplate.exchange(
                "/api/v1/admin/system/dynamic", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
    assertThat(dynamic.get("db_status")).isEqualTo("UP");
    assertThat(number(dynamic, "memory_used")).isGreaterThan(0);
    assertThat(number(dynamic, "disk_free")).isGreaterThan(0);
    assertThat(dynamic.get("cpu_usage_status")).isEqualTo("unsupported");

    Map<?, ?> update =
        data(
            restTemplate.exchange(
                "/api/v1/admin/system/check-update", HttpMethod.POST, new HttpEntity<>(headers), Map.class));
    assertThat(update.get("update_check_status")).isEqualTo("unsupported");
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
    String token = data(loginResponse).get("access_token").toString();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    return (Map<?, ?>) responseData(response);
  }

  private Object responseData(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }

  private long number(Map<?, ?> data, String key) {
    return ((Number) data.get(key)).longValue();
  }
}
