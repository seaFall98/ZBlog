package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
class Batch5AiArticleMetadataClosedLoopTest {

  @Autowired private TestRestTemplate restTemplate;

  private HttpServer aiServer;
  private String aiBaseUrl;
  private int aiStatus = 200;
  private String aiErrorBody = "";
  private String forcedAiContent;
  private String forcedSecondAiContent;
  private int aiRequestCount;

  @BeforeEach
  void startAiServer() throws IOException {
    aiServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    aiServer.createContext("/chat/completions", this::chat);
    aiServer.start();
    aiBaseUrl = "http://127.0.0.1:" + aiServer.getAddress().getPort();
  }

  @AfterEach
  void stopAiServer() {
    if (aiServer != null) {
      aiServer.stop(0);
    }
  }

  @Test
  void aiProviderErrorsReturnReadableMessage() {
    aiStatus = 400;
    aiErrorBody = "{\"error\":{\"message\":\"Model Not Exist\",\"type\":\"invalid_request_error\"}}";
    HttpHeaders headers = authenticatedHeaders();
    saveAiSettings(headers);

    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/ai/title",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("content", "Batch 5 error visibility"), headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("message").toString()).contains("Model Not Exist");
  }

  @Test
  void aiGenerationUsesSettingsSavedByAdminFrontendKeyFormat() {
    HttpHeaders headers = authenticatedHeaders();
    restTemplate.exchange(
        "/api/v1/admin/settings/ai",
        HttpMethod.PUT,
        new HttpEntity<>(
            Map.of(
                "ai.base_url", aiBaseUrl,
                "ai.api_key", "test-key",
                "ai.model", "test-model",
                "ai.title_prompt", "title prompt"),
            headers),
        Map.class);

    Map<?, ?> title =
        data(
            restTemplate.exchange(
                "/api/v1/admin/ai/title",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("content", "frontend key format"), headers),
                Map.class));

    assertThat(title.get("title")).isEqualTo("Generated Batch 5 Title");
  }

  @Test
  void overlongAiSummaryIsRegeneratedWithoutSemanticTruncation() {
    HttpHeaders headers = authenticatedHeaders();
    saveAiSettings(headers);
    forcedAiContent = "摘".repeat(260);
    forcedSecondAiContent = "这是重新压缩后的完整摘要，语义完整且没有被硬截断。";

    Map<?, ?> summary =
        data(
            restTemplate.exchange(
                "/api/v1/admin/ai/ai-summary",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("content", "Batch18 overlong summary"), headers),
                Map.class));

    assertThat(summary.get("summary")).isEqualTo(forcedSecondAiContent);
    assertThat(summary.get("trimmed")).isEqualTo(false);
    assertThat(summary.get("regenerated")).isEqualTo(true);
    assertThat(summary.get("over_limit")).isEqualTo(false);
    assertThat(summary.get("max_length")).isEqualTo(200);
    assertThat(summary.get("original_length")).isEqualTo(260);
    assertThat(summary.get("final_length")).isEqualTo(forcedSecondAiContent.length());
  }

  @Test
  void aiGeneratedTitleAndSummaryPersistThroughArticleSummaryWithoutFakeAiSummaryField() throws Exception {
    HttpHeaders headers = authenticatedHeaders();
    saveAiSettings(headers);

    String content = "# Batch 5\n\nAI metadata should survive article reload.";
    String generatedTitle =
        data(
                restTemplate.exchange(
                    "/api/v1/admin/ai/title",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("content", content), headers),
                    Map.class))
            .get("title")
            .toString();
    String generatedSummary =
        data(
                restTemplate.exchange(
                    "/api/v1/admin/ai/ai-summary",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("content", content), headers),
                    Map.class))
            .get("summary")
            .toString();

    ResponseEntity<Map> createResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "title", generatedTitle,
                    "slug", "batch-5-ai-metadata",
                    "content", content,
                    "summary", generatedSummary,
                    "is_publish", true),
                headers),
            Map.class);
    Map<?, ?> created = data(createResponse);
    Number articleId = (Number) created.get("id");
    assertThat(created.get("title")).isEqualTo("Generated Batch 5 Title");
    assertThat(created.get("summary")).isEqualTo("Generated Batch 5 Summary");
    assertThat(created.containsKey("ai_summary")).isFalse();

    Map<?, ?> adminDetail =
        data(
            restTemplate.exchange(
                "/api/v1/admin/articles/" + articleId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class));
    assertThat(adminDetail.get("title")).isEqualTo("Generated Batch 5 Title");
    assertThat(adminDetail.get("summary")).isEqualTo("Generated Batch 5 Summary");
    assertThat(adminDetail.containsKey("ai_summary")).isFalse();

    Map<?, ?> metadataOnlyUpdate =
        data(
            restTemplate.exchange(
                "/api/v1/admin/articles/" + articleId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("location", "Batch 5 Lab"), headers),
                Map.class));
    assertThat(metadataOnlyUpdate.get("summary")).isEqualTo("Generated Batch 5 Summary");
    assertThat(metadataOnlyUpdate.containsKey("ai_summary")).isFalse();

    data(
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("is_publish", true), headers),
            Map.class));

    Map<?, ?> publicArticle =
        data(restTemplate.getForEntity("/api/v1/articles/batch-5-ai-metadata", Map.class));
    assertThat(publicArticle.get("title")).isEqualTo("Generated Batch 5 Title");
    assertThat(publicArticle.get("summary")).isEqualTo("Generated Batch 5 Summary");
    assertThat(publicArticle.containsKey("ai_summary")).isFalse();

    assertThat(Files.readString(Path.of("../admin/src/views/article/ArticleForm.vue")))
        .doesNotContain("ai_summary");
    assertThat(Files.readString(Path.of("../admin/src/types/article.ts")))
        .doesNotContain("ai_summary");
    assertThat(Files.readString(Path.of("../blog/app/pages/posts/[slug].vue")))
        .doesNotContain("ai_summary");
    assertThat(Files.readString(Path.of("../blog/types/article.ts")))
        .doesNotContain("ai_summary");
  }

  private void saveAiSettings(HttpHeaders headers) {
    restTemplate.exchange(
        "/api/v1/admin/settings/ai",
        HttpMethod.PUT,
        new HttpEntity<>(
            Map.of(
                "base_url", aiBaseUrl,
                "api_key", "test-key",
                "model", "test-model",
                "summary_prompt", "summary prompt",
                "ai_summary_prompt", "ai summary prompt",
                "title_prompt", "title prompt"),
            headers),
        Map.class);
  }

  private void chat(HttpExchange exchange) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    if (aiStatus >= 400) {
      respond(exchange, aiStatus, aiErrorBody.getBytes(StandardCharsets.UTF_8));
      return;
    }
    aiRequestCount++;
    String content = forcedAiContent == null ? "Generated Batch 5 Summary" : forcedAiContent;
    if (forcedSecondAiContent != null && aiRequestCount > 1) {
      content = forcedSecondAiContent;
    }
    if (body.contains("title prompt")) {
      content = "Generated Batch 5 Title";
    }
    respond(
        exchange,
        200,
        ("{\"choices\":[{\"message\":{\"content\":\"" + content + "\"}}]}")
            .getBytes(StandardCharsets.UTF_8));
  }

  private void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
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
    Map<?, ?> body = response.getBody();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return (Map<?, ?>) body.get("data");
  }
}
