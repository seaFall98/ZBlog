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
    List<String> handlerNames = list.stream().map(row -> ((Map<?, ?>) row).get("name").toString()).toList();
    assertThat(handlerNames)
        .containsExactly(
            "article-scheduled-publish",
            "article-view-flush",
            "daily-visit-archive",
            "feedback-cleanup",
            "notification-cleanup",
            "seo-feed-refresh");

    ResponseEntity<Map> invalidCron =
        restTemplate.exchange(
            "/api/v1/admin/scheduled-jobs/" + jobId,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("cron_expression", "not-a-cron"), headers),
            Map.class);
    assertThat(invalidCron.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void batch8ScheduledJobsPublishDueArticlesAndArchiveVisits() {
    HttpHeaders headers = authenticatedHeaders();
    long dueArticleId =
        insertArticle(
            "batch8-scheduled-due-" + System.nanoTime(),
            "Batch8 Scheduled Due",
            "DRAFT",
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().minusMinutes(10));
    long manuallyUnpublishedId =
        insertArticle(
            "batch8-manual-draft-" + System.nanoTime(),
            "Batch8 Manual Draft",
            "DRAFT",
            LocalDateTime.now().minusDays(3),
            LocalDateTime.now().minusMinutes(1));
    insertVisit("batch8-archive-visitor-1", dueArticleId, LocalDateTime.now().minusDays(1).withHour(10));
    insertVisit("batch8-archive-visitor-2", dueArticleId, LocalDateTime.now().minusDays(1).withHour(11));

    ResponseEntity<Map> publish =
        restTemplate.exchange(
            "/api/v1/admin/scheduled-jobs/" + scheduledJobId("article-scheduled-publish") + "/run",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(publish.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(publish).get("status")).isEqualTo("success");
    assertThat(status(dueArticleId)).isEqualTo("PUBLISHED");
    assertThat(status(manuallyUnpublishedId)).isEqualTo("DRAFT");

    ResponseEntity<Map> archive =
        restTemplate.exchange(
            "/api/v1/admin/scheduled-jobs/" + scheduledJobId("daily-visit-archive") + "/run",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("parameters", Map.of("stat_date", LocalDateTime.now().minusDays(1).toLocalDate().toString())),
                headers),
            Map.class);
    assertThat(archive.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(archive).get("status")).isEqualTo("success");
    assertThat(
            jdbcTemplate.queryForObject(
                "select pv from stats_article_daily where stat_date = ? and article_id = ?",
                Long.class,
                LocalDateTime.now().minusDays(1).toLocalDate(),
                dueArticleId))
        .isEqualTo(2L);
  }

  private long notificationCleanupJobId() {
    return jdbcTemplate.queryForObject(
        "select id from scheduled_jobs where handler_name = 'notification-cleanup'", Long.class);
  }

  private long scheduledJobId(String handlerName) {
    return jdbcTemplate.queryForObject(
        "select id from scheduled_jobs where handler_name = ?", Long.class, handlerName);
  }

  private long insertArticle(
      String slug, String title, String status, LocalDateTime publishedAt, LocalDateTime updatedAt) {
    jdbcTemplate.update(
        """
        insert into articles (
          title, slug, content_markdown, content_html, content_text, summary,
          status, published_at, updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        title,
        slug,
        title + " markdown",
        "<p>" + title + "</p>",
        title + " text",
        title + " summary",
        status,
        publishedAt,
        updatedAt);
    return jdbcTemplate.queryForObject("select max(id) from articles where slug = ?", Long.class, slug);
  }

  private void insertVisit(String visitorId, long articleId, LocalDateTime createdAt) {
    jdbcTemplate.update(
        """
        insert into visit_events (
          visitor_id, event_type, url, hostname, title, referrer, language, screen,
          article_id, event_name, event_data, duration_seconds, ip, user_agent, created_at
        ) values (?, 'pageview', ?, 'localhost', 'Batch8 archive', '', 'zh-CN', '1440x900', ?, '', '{}', null, '127.0.0.1', 'JUnit', ?)
        """,
        visitorId,
        "/posts/batch8-scheduled",
        articleId,
        createdAt);
  }

  private String status(long articleId) {
    return jdbcTemplate.queryForObject("select status from articles where id = ?", String.class, articleId);
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
