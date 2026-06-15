package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
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
class P3SchedulerApiTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void notificationCleanupJobDeletesOnlyOldReadNotificationsAndWritesLog() {
    long oldReadId = insertNotification(true, LocalDateTime.now().minusDays(120));
    long recentReadId = insertNotification(true, LocalDateTime.now().minusDays(10));
    long oldUnreadId = insertNotification(false, null);
    long jobId = notificationCleanupJobId();
    HttpHeaders headers = authenticatedHeaders();

    ResponseEntity<Map> run =
        restTemplate.exchange(
            "/api/v1/admin/scheduled-jobs/" + jobId + "/run",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(run.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(run).get("status")).isEqualTo("success");
    assertThat(notificationExists(oldReadId)).isFalse();
    assertThat(notificationExists(recentReadId)).isTrue();
    assertThat(notificationExists(oldUnreadId)).isTrue();

    Map<?, ?> logs =
        data(
            restTemplate.exchange(
                "/api/v1/admin/scheduled-jobs/" + jobId + "/logs?page=1&page_size=5",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    assertThat((List<?>) logs.get("list")).isNotEmpty();
  }

  @Test
  void schedulerRejectsInvalidCronAndOnlyExposesWhitelistedHandlers() {
    long jobId = notificationCleanupJobId();
    HttpHeaders headers = authenticatedHeaders();

    Object handlers =
        dataObject(
            restTemplate.exchange(
                "/api/v1/admin/scheduled-jobs/handlers",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    List<?> list = (List<?>) handlers;
    assertThat(list).singleElement().satisfies(row -> assertThat(((Map<?, ?>) row).get("name")).isEqualTo("notification-cleanup"));

    ResponseEntity<Map> invalidCron =
        restTemplate.exchange(
            "/api/v1/admin/scheduled-jobs/" + jobId,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("cron_expression", "not-a-cron"), headers),
            Map.class);
    assertThat(invalidCron.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private long notificationCleanupJobId() {
    return jdbcTemplate.queryForObject(
        "select id from scheduled_jobs where handler_name = 'notification-cleanup'", Long.class);
  }

  private long insertNotification(boolean read, LocalDateTime readAt) {
    jdbcTemplate.update(
        """
        insert into notifications (type, title, content, link, data, is_read, read_at, sender, created_at)
        values ('comment_reply', 'cleanup test', 'cleanup test', '/posts/test#comment-1', '{}', ?, ?, 'test', ?)
        """,
        read,
        readAt,
        LocalDateTime.now().minusDays(120));
    return jdbcTemplate.queryForObject("select max(id) from notifications", Long.class);
  }

  private boolean notificationExists(long id) {
    Long count = jdbcTemplate.queryForObject("select count(*) from notifications where id = ?", Long.class, id);
    return count != null && count == 1L;
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(data(response).get("access_token").toString());
    return headers;
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(0);
    return (Map<?, ?>) response.getBody().get("data");
  }

  private Object dataObject(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("code")).isEqualTo(0);
    return response.getBody().get("data");
  }
}
