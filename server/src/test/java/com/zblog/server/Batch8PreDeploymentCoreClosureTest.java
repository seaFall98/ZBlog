package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch8PreDeploymentCoreClosureTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void articlePageviewBuffersArticleViewCountUntilFlushAndKeepsPublicCountFresh() {
    HttpHeaders headers = authenticatedHeaders();
    long articleId = insertArticle("batch8-view-count", "Batch 8 View Count", true, true, false, false, null, List.of(), "Batch8Place");

    Map<?, ?> beforeArticle = data(restTemplate.getForEntity("/api/v1/articles/batch8-view-count", Map.class));
    long beforeViews = number(beforeArticle, "view_count");
    long beforeSitePv = number(data(restTemplate.getForEntity("/api/v1/stats/site", Map.class)), "total_page_views");

    ResponseEntity<Map> collectResponse =
        restTemplate.postForEntity(
            "/api/v1/collect",
            Map.of(
                "type", "pageview",
                "url", "/posts/batch8-view-count",
                "hostname", "localhost",
                "title", "Batch 8 View Count",
                "screen", "1440x900",
                "language", "zh-CN",
                "article_id", articleId,
                "timestamp", System.currentTimeMillis()),
            Map.class);

    assertThat(collectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    Map<?, ?> afterArticle = data(restTemplate.getForEntity("/api/v1/articles/batch8-view-count", Map.class));
    assertThat(number(afterArticle, "view_count")).isEqualTo(beforeViews + 1);
    assertThat(jdbcTemplate.queryForObject("select view_count from articles where id = ?", Long.class, articleId))
        .isEqualTo(beforeViews);

    Map<?, ?> adminPage =
        data(
            restTemplate.exchange(
                "/api/v1/admin/articles?keyword=batch8-view-count",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    assertThat((List<?>) adminPage.get("list"))
        .anySatisfy(row -> assertThat(number((Map<?, ?>) row, "view_count")).isEqualTo(beforeViews));

    long flushJobId = scheduledJobId("article-view-flush");
    ResponseEntity<Map> flush =
        restTemplate.exchange(
            "/api/v1/admin/scheduled-jobs/" + flushJobId + "/run",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(flush.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(flush).get("status")).isEqualTo("success");
    assertThat(jdbcTemplate.queryForObject("select view_count from articles where id = ?", Long.class, articleId))
        .isEqualTo(beforeViews + 1);

    long afterSitePv = number(data(restTemplate.getForEntity("/api/v1/stats/site", Map.class)), "total_page_views");
    assertThat(afterSitePv).isEqualTo(beforeSitePv + 1);
  }

  @Test
  void adminVisitListFiltersAndParsesBrowserAndOsWithoutFakeGeo() {
    HttpHeaders headers = authenticatedHeaders();
    insertVisit(
        "batch8-visitor-chrome",
        "203.0.113.10",
        "/posts/batch8-visit-target",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36",
        LocalDate.now().atTime(12, 0));
    insertVisit(
        "batch8-visitor-safari",
        "203.0.113.20",
        "/posts/batch8-visit-other-target",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15",
        LocalDate.now().atTime(12, 0));

    Map<?, ?> byKeyword = adminGet("/api/v1/admin/stats/visits?keyword=batch8-visit-target", headers);
    assertVisitUrls(byKeyword, "/posts/batch8-visit-target");

    Map<?, ?> defaultList = adminGet("/api/v1/admin/stats/visits?page=1&page_size=20", headers);
    assertThat((List<?>) defaultList.get("list")).isNotEmpty();

    Map<?, ?> byVisitor = adminGet("/api/v1/admin/stats/visits?visitor_id=batch8-visitor-chrome", headers);
    assertVisitVisitors(byVisitor, "batch8-visitor-chrome");

    Map<?, ?> byIp = adminGet("/api/v1/admin/stats/visits?ip=203.0.113.10", headers);
    assertVisitIps(byIp, "203.0.113.10");

    Map<?, ?> excludingIp = adminGet("/api/v1/admin/stats/visits?keyword=batch8-visit&exclude_ips=203.0.113.20", headers);
    assertVisitIps(excludingIp, "203.0.113.10");

    String today = LocalDate.now().toString();
    Map<?, ?> byDate =
        adminGet("/api/v1/admin/stats/visits?keyword=batch8-visit&start_time=" + today + "&end_time=" + today, headers);
    assertThat((List<?>) byDate.get("list")).isNotEmpty();
    assertThat(first(byDate).get("created_at").toString()).startsWith(today);
    Map<?, ?> byPreciseDateTime =
        adminGet(
            "/api/v1/admin/stats/visits?keyword=batch8-visit-target&start_time="
                + LocalDate.now().atStartOfDay()
                + "&end_time="
                + LocalDate.now().atTime(23, 59, 59),
            headers);
    assertVisitUrls(byPreciseDateTime, "/posts/batch8-visit-target");

    Map<?, ?> byBrowser = adminGet("/api/v1/admin/stats/visits?browser=Chrome", headers);
    assertVisitVisitors(byBrowser, "batch8-visitor-chrome");
    assertThat(first(byBrowser).get("browser")).isEqualTo("Chrome");

    Map<?, ?> byOs = adminGet("/api/v1/admin/stats/visits?os=Windows", headers);
    assertVisitVisitors(byOs, "batch8-visitor-chrome");
    assertThat(first(byOs).get("os")).isEqualTo("Windows");
    assertThat(first(byOs).get("location")).isEqualTo("unsupported");
  }

  @Test
  void adminArticleFiltersMatchCurrentUiContract() {
    HttpHeaders headers = authenticatedHeaders();
    long categoryId = insertCategory("Batch 8 Category", "batch8-category");
    long tagId = insertTag("Batch 8 Tag", "batch8-tag");
    insertArticle("batch8-article-match", "Batch 8 Article Match", true, true, true, true, categoryId, List.of(tagId), "Batch8City");
    insertArticle("batch8-article-other", "Batch 8 Article Other", true, false, false, false, null, List.of(), "OtherCity");

    assertArticleSlugs(adminGet("/api/v1/admin/articles?category_id=" + categoryId, headers), "batch8-article-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?tag_ids=" + tagId, headers), "batch8-article-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?location=Batch8City", headers), "batch8-article-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?is_top=true&keyword=batch8-article", headers), "batch8-article-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?is_essence=true&keyword=batch8-article", headers), "batch8-article-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?is_outdated=true&keyword=batch8-article", headers), "batch8-article-match");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?is_publish=true&keyword=Batch%208%20Article", headers), "batch8-article-match", "batch8-article-other");
    assertArticleSlugs(adminGet("/api/v1/admin/articles?start_time=" + LocalDate.now() + "&end_time=" + LocalDate.now() + "&keyword=Batch%208%20Article", headers), "batch8-article-match", "batch8-article-other");
  }

  @Test
  void adminCommentFiltersAndPublicCommentPrivacyMatchContract() {
    HttpHeaders headers = authenticatedHeaders();
    long parent =
        insertComment(
            "article",
            "batch8-comment-target",
            null,
            "batch8 visible parent",
            1,
            false,
            LocalDate.now().atTime(12, 0),
            "Shanghai",
            "Chrome",
            "Windows");
    long hidden =
        insertComment(
            "article",
            "batch8-comment-target",
            null,
            "batch8 hidden parent",
            0,
            false,
            LocalDate.now().atTime(12, 0),
            "Beijing",
            "Firefox",
            "Linux");
    long deleted =
        insertComment(
            "article",
            "batch8-comment-target",
            null,
            "batch8 deleted parent",
            1,
            true,
            LocalDate.now().atTime(12, 0),
            "Shenzhen",
            "Safari",
            "macOS");
    long child =
        insertComment(
            "article",
            "batch8-comment-target",
            parent,
            "batch8 child reply",
            1,
            false,
            LocalDate.now().atTime(12, 0),
            "Hangzhou",
            "Edge",
            "Windows");

    assertCommentIds(adminGet("/api/v1/admin/comments?keyword=visible", headers), parent);
    assertCommentIds(adminGet("/api/v1/admin/comments?status=0", headers), hidden);
    assertCommentIds(adminGet("/api/v1/admin/comments?is_deleted=true", headers), deleted);
    assertCommentIds(adminGet("/api/v1/admin/comments?is_sub=true", headers), child);
    assertCommentIds(adminGet("/api/v1/admin/comments?start_time=" + LocalDate.now() + "&end_time=" + LocalDate.now(), headers), parent, hidden, deleted, child);
    assertCommentIds(
        adminGet(
            "/api/v1/admin/comments?start_time="
                + LocalDate.now().atStartOfDay()
                + "&end_time="
                + LocalDate.now().atTime(23, 59, 59),
            headers),
        parent,
        hidden,
        deleted,
        child);

    Map<?, ?> publicComments =
        data(
            restTemplate.getForEntity(
                "/api/v1/comments?target_type=article&target_key=batch8-comment-target&page=1&page_size=20",
                Map.class));
    Map<?, ?> publicParent = first(publicComments);
    assertThat(publicParent.containsKey("location")).isFalse();
    assertThat(publicParent.containsKey("browser")).isFalse();
    assertThat(publicParent.containsKey("os")).isFalse();
  }

  @Test
  void adminFileFiltersMatchCurrentUiContract() {
    HttpHeaders headers = authenticatedHeaders();
    long image = insertFile("batch8-image.png", "batch8 original image.png", "image/png", 500L, "article-image", 1, LocalDate.now().atTime(12, 0));
    long pdf = insertFile("batch8-doc.pdf", "batch8 original doc.pdf", "application/pdf", 2_000_000L, "feedback-attachment", 0, LocalDate.now().atTime(12, 0));
    long usedBySetting =
        insertFile(
            "batch8-used-by-setting.png",
            "batch8 used by setting.png",
            "image/png",
            700L,
            "site-avatar",
            0,
            LocalDate.now().atTime(12, 0));
    jdbcTemplate.update(
        """
        insert into settings (group_name, key_name, value_text)
        values ('batch8', 'batch8.used_file', '/uploads/batch8-used-by-setting.png')
        """);

    assertFileIds(adminGet("/api/v1/admin/files?keyword=batch8-image", headers), image);
    assertFileIds(adminGet("/api/v1/admin/files?file_type=image&keyword=batch8-image", headers), image);
    assertFileIds(adminGet("/api/v1/admin/files?status=0&keyword=batch8-doc", headers), pdf);
    assertFileIds(adminGet("/api/v1/admin/files?status=1&keyword=batch8-used-by-setting", headers), usedBySetting);
    assertThat(number(first(adminGet("/api/v1/admin/files?keyword=batch8-used-by-setting", headers)), "status"))
        .isEqualTo(1);
    assertFileIds(adminGet("/api/v1/admin/files?upload_type=feedback-attachment", headers), pdf);
    assertFileIds(adminGet("/api/v1/admin/files?min_size=1000000&max_size=3000000&keyword=batch8-doc", headers), pdf);
    assertFileIds(
        adminGet(
            "/api/v1/admin/files?start_time=" + LocalDate.now() + "&end_time=" + LocalDate.now() + "&keyword=batch8",
            headers),
        image,
        pdf,
        usedBySetting);
    assertFileIds(
        adminGet(
            "/api/v1/admin/files?start_time="
                + LocalDate.now().atStartOfDay()
                + "&end_time="
                + LocalDate.now().atTime(23, 59, 59)
                + "&keyword=batch8",
            headers),
        image,
        pdf,
        usedBySetting);
  }

  private Map<?, ?> adminGet(String path, HttpHeaders headers) {
    ResponseEntity<Map> response =
        restTemplate.exchange(uri(path), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    return data(response);
  }

  private URI uri(String path) {
    return UriComponentsBuilder.fromUriString(path).build(true).toUri();
  }

  private void assertVisitUrls(Map<?, ?> page, String... expectedUrls) {
    assertValues(page, "url", expectedUrls);
  }

  private void assertVisitVisitors(Map<?, ?> page, String... expectedVisitors) {
    assertValues(page, "visitor_id", expectedVisitors);
  }

  private void assertVisitIps(Map<?, ?> page, String... expectedIps) {
    assertValues(page, "ip", expectedIps);
  }

  private void assertArticleSlugs(Map<?, ?> page, String... expectedSlugs) {
    assertValues(page, "slug", expectedSlugs);
  }

  private void assertCommentIds(Map<?, ?> page, long... expectedIds) {
    List<?> list = (List<?>) page.get("list");
    Long[] boxed = Arrays.stream(expectedIds).boxed().toArray(Long[]::new);
    assertThat(list).extracting(row -> ((Number) ((Map<?, ?>) row).get("id")).longValue()).containsExactlyInAnyOrder(boxed);
  }

  private void assertFileIds(Map<?, ?> page, long... expectedIds) {
    List<?> list = (List<?>) page.get("list");
    Long[] boxed = Arrays.stream(expectedIds).boxed().toArray(Long[]::new);
    assertThat(list).extracting(row -> ((Number) ((Map<?, ?>) row).get("id")).longValue()).containsExactlyInAnyOrder(boxed);
  }

  private void assertValues(Map<?, ?> page, String key, String... expectedValues) {
    List<?> list = (List<?>) page.get("list");
    assertThat(list).extracting(row -> ((Map<?, ?>) row).get(key).toString()).containsExactlyInAnyOrder(expectedValues);
  }

  private Map<?, ?> first(Map<?, ?> page) {
    List<?> list = (List<?>) page.get("list");
    assertThat(list).isNotEmpty();
    return (Map<?, ?>) list.getFirst();
  }

  private long insertCategory(String name, String slug) {
    return insert(
        "insert into categories (name, slug, description, sort_order) values (?, ?, '', 0)",
        name,
        slug);
  }

  private long insertTag(String name, String slug) {
    return insert("insert into tags (name, slug, description) values (?, ?, '')", name, slug);
  }

  private long insertArticle(
      String slug,
      String title,
      boolean published,
      boolean top,
      boolean essence,
      boolean outdated,
      Long categoryId,
      List<Long> tagIds,
      String location) {
    long id =
        insert(
            """
            insert into articles (
              slug, title, summary, cover_url, content_markdown, content_html, content_text,
              status, category_id, is_top, is_essence, is_outdated, location, published_at
            ) values (?, ?, ?, '', ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)
            """,
            slug,
            title,
            "summary " + slug,
            "# " + title,
            "<h1>" + title + "</h1>",
            title + " body",
            published ? "PUBLISHED" : "DRAFT",
            categoryId,
            top,
            essence,
            outdated,
            location);
    for (Long tagId : tagIds) {
      jdbcTemplate.update("insert into article_tags (article_id, tag_id) values (?, ?)", id, tagId);
    }
    return id;
  }

  private long insertVisit(String visitorId, String ip, String url, String userAgent, LocalDateTime createdAt) {
    return insert(
        """
        insert into visit_events (
          visitor_id, event_type, url, hostname, title, referrer, language, screen,
          article_id, event_name, event_data, duration_seconds, ip, user_agent, created_at
        ) values (?, 'pageview', ?, 'localhost', ?, '', 'zh-CN', '1440x900', null, '', '{}', null, ?, ?, ?)
        """,
        visitorId,
        url,
        "Visit " + visitorId,
        ip,
        userAgent,
        createdAt);
  }

  private long insertComment(
      String targetType,
      String targetKey,
      Long parentId,
      String content,
      int status,
      boolean deleted,
      LocalDateTime createdAt,
      String location,
      String browser,
      String os) {
    return insert(
        """
        insert into comments (
          target_type, target_key, parent_id, content, status, is_deleted, nickname, email,
          location, browser, os, created_at, deleted_at
        ) values (?, ?, ?, ?, ?, ?, 'Batch8', 'batch8@example.com', ?, ?, ?, ?, ?)
        """,
        targetType,
        targetKey,
        parentId,
        content,
        status,
        deleted,
        location,
        browser,
        os,
        createdAt,
        deleted ? createdAt.plusMinutes(1) : null);
  }

  private long insertFile(
      String filename,
      String originalName,
      String fileType,
      long fileSize,
      String uploadType,
      int status,
      LocalDateTime uploadTime) {
    return insert(
        """
        insert into files (filename, original_name, file_url, file_type, file_size, upload_type, status, upload_time)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        filename,
        originalName,
        "/uploads/" + filename,
        fileType,
        fileSize,
        uploadType,
        status,
        uploadTime);
  }

  private long insert(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
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

  private long scheduledJobId(String handlerName) {
    return jdbcTemplate.queryForObject(
        "select id from scheduled_jobs where handler_name = ?", Long.class, handlerName);
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return (Map<?, ?>) body.get("data");
  }

  private long number(Map<?, ?> data, String key) {
    return ((Number) data.get(key)).longValue();
  }
}
