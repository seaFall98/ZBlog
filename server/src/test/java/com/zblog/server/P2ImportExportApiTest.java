package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
class P2ImportExportApiTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void articlesCanBeImportedExportedForWechatAndDownloadedAsZip() throws Exception {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("source_type", "hexo");
    body.add(
        "files",
        resource(
            "imported-article.md",
            """
            ---
            title: Imported P2 Article
            description: Imported summary
            category: Imported Category
            tags: [imported, p2]
            ---
            # Imported P2 Article

            Imported **markdown** body.
            """));

    ResponseEntity<Map> importResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/import", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = (Map<?, ?>) data(importResponse);
    assertThat(result.get("total")).isEqualTo(1);
    assertThat(result.get("success")).isEqualTo(1);
    assertThat(result.get("failed")).isEqualTo(0);
    assertThat(result.get("categories_added")).isEqualTo(1);
    assertThat(result.get("tags_added")).isEqualTo(2);

    HttpHeaders authHeaders = authenticatedHeaders();
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles?page_size=100", HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class);
    Map<?, ?> page = (Map<?, ?>) data(listResponse);
    Map<?, ?> article =
        (Map<?, ?>)
            ((List<?>) page.get("list"))
                .stream()
                    .filter(item -> "Imported P2 Article".equals(((Map<?, ?>) item).get("title")))
                    .findFirst()
                    .orElseThrow();
    Number articleId = (Number) article.get("id");
    assertThat(article.get("content_markdown")).asString().contains("# Imported P2 Article");
    assertThat(article.get("content")).asString().contains("<p>Imported **markdown** body.</p>");

    ResponseEntity<Map> wechatResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/wechat/export",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders),
            Map.class);
    assertThat(wechatResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(((Map<?, ?>) data(wechatResponse)).get("html")).asString().contains("<h1");

    ResponseEntity<byte[]> zipResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/download/zip",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders),
            byte[].class);
    assertThat(zipResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(zipContent(zipResponse.getBody())).contains("Imported **markdown** body.");
  }

  @Test
  void commentsCanBeImportedAndListedPublicly() {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("source_type", "artalk");
    body.add(
        "file",
        resource(
            "comments.json",
            """
            {
              "comments": [
                {
                  "target_type": "article",
                  "target_key": "p2-comment-target",
                  "content": "Imported P2 comment",
                  "nick": "Importer",
                  "email": "importer@example.com",
                  "link": "https://importer.example.com"
                }
              ]
            }
            """));

    ResponseEntity<Map> importResponse =
        restTemplate.exchange(
            "/api/v1/admin/comments/import", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = (Map<?, ?>) data(importResponse);
    assertThat(result.get("total")).isEqualTo(1);
    assertThat(result.get("success")).isEqualTo(1);
    assertThat(result.get("failed")).isEqualTo(0);

    ResponseEntity<Map> publicList =
        restTemplate.getForEntity(
            "/api/v1/comments?target_type=article&target_key=p2-comment-target", Map.class);
    List<?> comments = (List<?>) ((Map<?, ?>) data(publicList)).get("list");
    assertThat(comments)
        .anySatisfy(
            comment ->
                assertThat(((Map<?, ?>) comment).get("content")).isEqualTo("Imported P2 comment"));
  }

  private ByteArrayResource resource(String filename, String content) {
    return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return filename;
      }
    };
  }

  private String zipContent(byte[] bytes) throws Exception {
    assertThat(bytes).isNotNull();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
      ZipEntry entry = zip.getNextEntry();
      assertThat(entry).isNotNull();
      return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
    }
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
