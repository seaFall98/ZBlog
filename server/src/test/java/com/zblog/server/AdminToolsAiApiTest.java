package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class AdminToolsAiApiTest {

  @Autowired private TestRestTemplate restTemplate;

  private HttpServer remoteServer;
  private String remoteBaseUrl;

  @BeforeEach
  void startRemoteServer() throws IOException {
    remoteServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    remoteServer.createContext("/page", this::page);
    remoteServer.createContext("/image.png", this::image);
    remoteServer.createContext("/chat/completions", this::chat);
    remoteServer.start();
    remoteBaseUrl = "http://127.0.0.1:" + remoteServer.getAddress().getPort();
  }

  @AfterEach
  void stopRemoteServer() {
    remoteServer.stop(0);
  }

  @Test
  void adminToolsFetchLinkMetaParseVideoAndDownloadImage() {
    HttpHeaders headers = authenticatedHeaders();

    ResponseEntity<Map> metaResponse =
        restTemplate.exchange(
            "/api/v1/admin/tools/fetch-linkmeta",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("url", remoteBaseUrl + "/page"), headers),
            Map.class);
    Map<?, ?> meta = data(metaResponse);
    assertThat(meta.get("title")).isEqualTo("ZBlog Demo");
    assertThat(meta.get("description")).isEqualTo("Demo description");
    assertThat(meta.get("favicon")).isEqualTo(remoteBaseUrl + "/image.png");

    ResponseEntity<Map> videoResponse =
        restTemplate.exchange(
            "/api/v1/admin/tools/parse-video",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("url", "https://www.bilibili.com/video/BV1xx411c7mD"), headers),
            Map.class);
    Map<?, ?> video = data(videoResponse);
    assertThat(video.get("platform")).isEqualTo("bilibili");
    assertThat(video.get("video_id")).isEqualTo("BV1xx411c7mD");

    ResponseEntity<Map> imageResponse =
        restTemplate.exchange(
            "/api/v1/admin/tools/download-image",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("url", remoteBaseUrl + "/image.png"), headers),
            Map.class);
    Map<?, ?> image = data(imageResponse);
    assertThat(image.get("content_type")).isEqualTo("image/png");
    assertThat(image.get("content_length")).isEqualTo(4);
    assertThat(Base64.getDecoder().decode((String) image.get("data"))).containsExactly(1, 2, 3, 4);
  }

  @Test
  void adminAiUsesSavedSettingsAndOpenAiCompatibleResponse() {
    HttpHeaders headers = authenticatedHeaders();
    restTemplate.exchange(
        "/api/v1/admin/settings/ai",
        HttpMethod.PUT,
        new HttpEntity<>(
            Map.of(
                "base_url", remoteBaseUrl,
                "api_key", "test-key",
                "model", "test-model",
                "summary_prompt", "summary prompt",
                "ai_summary_prompt", "ai summary prompt",
                "title_prompt", "title prompt"),
            headers),
        Map.class);

    ResponseEntity<Map> testResponse =
        restTemplate.exchange(
            "/api/v1/admin/ai/test",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("base_url", remoteBaseUrl, "api_key", "test-key", "model", "test-model"), headers),
            Map.class);
    assertThat(data(testResponse).get("status")).isEqualTo("ok");

    ResponseEntity<Map> summaryResponse =
        restTemplate.exchange(
            "/api/v1/admin/ai/summary",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("content", "Article content"), headers),
            Map.class);
    assertThat(data(summaryResponse).get("summary")).isEqualTo("Generated summary");

    ResponseEntity<Map> aiSummaryResponse =
        restTemplate.exchange(
            "/api/v1/admin/ai/ai-summary",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("content", "Article content"), headers),
            Map.class);
    assertThat(data(aiSummaryResponse).get("summary")).isEqualTo("Generated AI summary");

    ResponseEntity<Map> titleResponse =
        restTemplate.exchange(
            "/api/v1/admin/ai/title",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("content", "Article content"), headers),
            Map.class);
    assertThat(data(titleResponse).get("title")).isEqualTo("Generated title");
  }

  private void page(HttpExchange exchange) throws IOException {
    respond(
        exchange,
        "text/html; charset=utf-8",
        """
        <html><head>
          <title>ZBlog Demo</title>
          <meta name=\"description\" content=\"Demo description\">
          <link rel=\"icon\" href=\"/image.png\">
        </head><body>ok</body></html>
        """.getBytes(StandardCharsets.UTF_8));
  }

  private void image(HttpExchange exchange) throws IOException {
    respond(exchange, "image/png", new byte[] {1, 2, 3, 4});
  }

  private void chat(HttpExchange exchange) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    String content = "Generated summary";
    if (body.contains("ai summary prompt")) {
      content = "Generated AI summary";
    } else if (body.contains("title prompt")) {
      content = "Generated title";
    } else if (body.contains("请回复 OK")) {
      content = "OK";
    }
    respond(
        exchange,
        "application/json",
        ("{\"choices\":[{\"message\":{\"content\":\"" + content + "\"}}]}")
            .getBytes(StandardCharsets.UTF_8));
  }

  private void respond(HttpExchange exchange, String contentType, byte[] body) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(200, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
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
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return (Map<?, ?>) body.get("data");
  }
}
