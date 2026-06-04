package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.zblog.rssfeed.application.RssFeedAdminService;
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
class Batch4RssReaderClosedLoopTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private RssFeedAdminService rssFeedAdminService;

  private HttpServer server;

  @BeforeEach
  void clearBatch4Sources() {
    jdbcTemplate.update(
        "delete from rss_feed_articles where friend_id in (select id from friends where name like 'Batch 4%')");
    jdbcTemplate.update("delete from friends where name like 'Batch 4%'");
  }

  @AfterEach
  void cleanupBatch4Sources() {
    if (server != null) {
      server.stop(0);
    }
    clearBatch4Sources();
  }

  @Test
  void manualRefreshFetchesParsesPersistsDeduplicatesAndPreservesReadState() throws Exception {
    startServer(
        "/rss.xml",
        200,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Batch 4 Feed</title>
            <item>
              <title>Batch 4 RSS Item</title>
              <link>https://example.com/batch4-rss-item</link>
              <description>RSS item imported by Batch 4</description>
              <pubDate>Sat, 16 May 2026 03:00:00 GMT</pubDate>
            </item>
          </channel>
        </rss>
        """);
    long friendId = createFriend("Batch 4 RSS Source", feedUrl("/rss.xml"));
    HttpHeaders headers = authenticatedHeaders();

    ResponseEntity<Map> refreshResponse =
        restTemplate.exchange(
            "/api/v1/admin/rssfeed/refresh",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> refresh = data(refreshResponse);
    assertThat(refresh.get("fetched")).isEqualTo(1);
    assertThat(refresh.get("inserted")).isEqualTo(1);
    assertThat(refresh.get("failed")).isEqualTo(0);

    Map<?, ?> firstItem = firstRssItem("Batch 4 RSS Item", headers);
    assertThat(((Number) firstItem.get("friend_id")).longValue()).isEqualTo(friendId);
    assertThat(firstItem.get("link")).isEqualTo("https://example.com/batch4-rss-item");
    assertThat(firstItem.get("description")).isEqualTo("RSS item imported by Batch 4");
    assertThat(firstItem.get("is_read")).isEqualTo(false);

    restTemplate.exchange(
        "/api/v1/admin/rssfeed/" + firstItem.get("id") + "/read",
        HttpMethod.PUT,
        new HttpEntity<>(headers),
        Map.class);
    assertThat(firstRssItem("Batch 4 RSS Item", headers).get("is_read")).isEqualTo(true);

    ResponseEntity<Map> duplicateRefresh =
        restTemplate.exchange(
            "/api/v1/admin/rssfeed/refresh",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);
    Map<?, ?> duplicateResult = data(duplicateRefresh);
    assertThat(duplicateResult.get("fetched")).isEqualTo(1);
    assertThat(duplicateResult.get("inserted")).isEqualTo(0);
    assertThat(firstRssItem("Batch 4 RSS Item", headers).get("is_read")).isEqualTo(true);
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from rss_feed_articles where link = ?",
            Integer.class,
            "https://example.com/batch4-rss-item");
    assertThat(count).isEqualTo(1);
  }

  @Test
  void scheduledRefreshReusesManualRefreshPath() throws Exception {
    startServer(
        "/scheduled.xml",
        200,
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Batch 18 Scheduled Feed</title>
            <item>
              <title>Batch 18 Scheduled RSS Item</title>
              <link>https://example.com/batch18-scheduled-rss-item</link>
              <description>RSS item imported by scheduled refresh</description>
              <pubDate>Sat, 23 May 2026 03:00:00 GMT</pubDate>
            </item>
          </channel>
        </rss>
        """);
    createFriend("Batch 4 Scheduled RSS Source", feedUrl("/scheduled.xml"));

    Map<?, ?> refresh = rssFeedAdminService.scheduledRefresh();

    assertThat(refresh.get("fetched")).isEqualTo(1);
    assertThat(refresh.get("inserted")).isEqualTo(1);
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from rss_feed_articles where link = ?",
            Integer.class,
            "https://example.com/batch18-scheduled-rss-item");
    assertThat(count).isEqualTo(1);
  }

  @Test
  void refreshFailureRecordsExplicitSourceError() throws Exception {
    startServer("/broken.xml", 500, "broken feed");
    long friendId = createFriend("Batch 4 Broken RSS Source", feedUrl("/broken.xml"));
    HttpHeaders headers = authenticatedHeaders();

    ResponseEntity<Map> refreshResponse =
        restTemplate.exchange(
            "/api/v1/admin/rssfeed/refresh",
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> refresh = data(refreshResponse);
    assertThat(refresh.get("failed")).isEqualTo(1);
    Map<String, Object> source = friend(friendId);
    assertThat(source.get("rss_status")).isEqualTo("failed");
    assertThat(source.get("rss_last_error").toString()).contains("HTTP 500");
  }

  private void startServer(String path, int status, String body) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        path,
        exchange -> {
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/rss+xml; charset=utf-8");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  private String feedUrl(String path) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + path;
  }

  private long createFriend(String name, String rssUrl) {
    org.springframework.jdbc.support.KeyHolder keyHolder =
        new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
                  insert into friends (name, url, description, avatar, sort_order, is_pending, rss_url, accessible)
                  values (?, ?, '', '', 0, false, ?, 1)
                  """,
                  java.sql.Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, name);
          statement.setString(2, "https://example.com/" + name.replace(" ", "-").toLowerCase());
          statement.setString(3, rssUrl);
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }

  private Map<String, Object> friend(long id) {
    return jdbcTemplate.queryForMap("select * from friends where id = ?", id);
  }

  private Map<?, ?> firstRssItem(String keyword, HttpHeaders headers) {
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/rssfeed?page=1&page_size=100",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);
    Map<?, ?> page = data(listResponse);
    List<?> list = (List<?>) page.get("list");
    return list.stream()
        .map(Map.class::cast)
        .filter(item -> keyword.equals(item.get("title")))
        .findFirst()
        .orElseThrow();
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> loginResponse =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
    String token = (String) data(loginResponse).get("access_token");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Map<?, ?> data(ResponseEntity<Map> response) {
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return (Map<?, ?>) body.get("data");
  }
}
