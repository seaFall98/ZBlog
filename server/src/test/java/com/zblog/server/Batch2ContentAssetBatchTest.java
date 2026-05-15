package com.zblog.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
class Batch2ContentAssetBatchTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void uploadCreatesDbRowServedAssetAndDeleteMakesAssetUnavailable() throws Exception {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", resource("batch2-upload.png", "batch2 image"));
    body.add("type", "image");

    ResponseEntity<Map> uploadResponse =
        restTemplate.exchange(
            "/api/v1/admin/files", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> upload = (Map<?, ?>) data(uploadResponse);
    Number fileId = (Number) upload.get("id");
    String fileUrl = upload.get("file_url").toString();
    String fileName = upload.get("file_name").toString();
    assertThat(upload.get("original_name")).isEqualTo("batch2-upload.png");
    assertThat(Files.exists(Path.of("uploads").toAbsolutePath().normalize().resolve(fileName))).isTrue();

    ResponseEntity<byte[]> assetResponse = restTemplate.getForEntity(fileUrl, byte[].class);
    assertThat(assetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new String(assetResponse.getBody(), StandardCharsets.UTF_8)).isEqualTo("batch2 image");

    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/files", HttpMethod.GET, new HttpEntity<>(authenticatedHeaders()), Map.class);
    List<?> files = (List<?>) ((Map<?, ?>) data(listResponse)).get("list");
    assertThat(files).anySatisfy(file -> assertThat(((Map<?, ?>) file).get("id")).isEqualTo(fileId));

    ResponseEntity<Map> deleteResponse =
        restTemplate.exchange(
            "/api/v1/admin/files/" + fileId,
            HttpMethod.DELETE,
            new HttpEntity<>(authenticatedHeaders()),
            Map.class);
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> afterDeleteList =
        restTemplate.exchange(
            "/api/v1/admin/files", HttpMethod.GET, new HttpEntity<>(authenticatedHeaders()), Map.class);
    List<?> remainingFiles = (List<?>) ((Map<?, ?>) data(afterDeleteList)).get("list");
    assertThat(remainingFiles).noneSatisfy(file -> assertThat(((Map<?, ?>) file).get("id")).isEqualTo(fileId));
    assertThat(Files.exists(Path.of("uploads").toAbsolutePath().normalize().resolve(fileName))).isFalse();
    assertThat(restTemplate.getForEntity(fileUrl, byte[].class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void publicUploadReturnsFrontendContractWithoutAuthentication() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", resource("comment-image.png", "comment image"));
    body.add("type", "评论贴图");

    ResponseEntity<Map> uploadResponse =
        restTemplate.exchange("/api/v1/upload", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> upload = (Map<?, ?>) data(uploadResponse);
    assertThat(upload.get("original_name")).isEqualTo("comment-image.png");
    assertThat(upload.get("file_url")).asString().startsWith("/uploads/");

    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/files", HttpMethod.GET, new HttpEntity<>(authenticatedHeaders()), Map.class);
    List<?> files = (List<?>) ((Map<?, ?>) data(listResponse)).get("list");
    assertThat(files)
        .anySatisfy(
            file -> {
              Map<?, ?> item = (Map<?, ?>) file;
              assertThat(item.get("id")).isEqualTo(upload.get("id"));
              assertThat(item.get("upload_type")).isEqualTo("评论贴图");
            });
  }

  @Test
  void markdownImportPreservesServedImageAndZipContainsUsableMarkdown() throws Exception {
    HttpHeaders uploadHeaders = authenticatedHeaders();
    uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
    uploadBody.add("file", resource("imported-image.png", "imported image"));
    uploadBody.add("type", "image");
    String imageUrl =
        ((Map<?, ?>)
                data(
                    restTemplate.exchange(
                        "/api/v1/admin/files",
                        HttpMethod.POST,
                        new HttpEntity<>(uploadBody, uploadHeaders),
                        Map.class)))
            .get("file_url")
            .toString();

    HttpHeaders importHeaders = authenticatedHeaders();
    importHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> importBody = new LinkedMultiValueMap<>();
    importBody.add("source_type", "markdown");
    importBody.add(
        "files",
        resource(
            "image-import.md",
            """
            # Batch2 Image Import

            ![Imported Image](%s)
            """
                .formatted(imageUrl)));

    ResponseEntity<Map> importResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/import",
            HttpMethod.POST,
            new HttpEntity<>(importBody, importHeaders),
            Map.class);
    assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> importResult = (Map<?, ?>) data(importResponse);
    assertThat(importResult.get("success")).isEqualTo(1);
    assertThat(importResult.get("failed")).isEqualTo(0);

    Map<?, ?> article = adminArticleByTitle("Batch2 Image Import");
    Number articleId = (Number) article.get("id");
    assertThat(article.get("content_markdown")).asString().contains("![Imported Image](" + imageUrl + ")");
    assertThat(article.get("content")).asString().contains("<img").contains(imageUrl);
    assertThat(restTemplate.getForEntity(imageUrl, byte[].class).getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<byte[]> zipResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/" + articleId + "/download/zip",
            HttpMethod.GET,
            new HttpEntity<>(authenticatedHeaders()),
            byte[].class);
    assertThat(zipResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<ZipContent> zipContents = zipContents(zipResponse.getBody());
    assertThat(zipContents)
        .anySatisfy(
            entry -> {
              assertThat(entry.name()).endsWith(".md");
              assertThat(new String(entry.bytes(), StandardCharsets.UTF_8))
                  .contains("![Imported Image](assets/" + imageUrl.substring("/uploads/".length()) + ")");
            });
    assertThat(zipContents)
        .anySatisfy(
            entry -> {
              assertThat(entry.name()).isEqualTo("assets/" + imageUrl.substring("/uploads/".length()));
              assertThat(new String(entry.bytes(), StandardCharsets.UTF_8)).isEqualTo("imported image");
            });
  }

  @Test
  void publicUploadRejectsUnknownTypeInsteadOfSilentlyRelabeling() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", resource("unknown-type.png", "unknown type"));
    body.add("type", "文章视频");

    ResponseEntity<Map> uploadResponse =
        restTemplate.exchange("/api/v1/upload", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(uploadResponse.getBody()).containsEntry("code", 40001);
  }

  @Test
  void markdownImportReportsUnsupportedRemoteImageInsteadOfCreatingBrokenArticle() {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("source_type", "markdown");
    body.add(
        "files",
        resource(
            "remote-image.md",
            """
            # Remote Image Import

            ![Remote](https://example.com/remote.png)
            """));

    ResponseEntity<Map> importResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/import", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = (Map<?, ?>) data(importResponse);
    assertThat(result.get("success")).isEqualTo(0);
    assertThat(result.get("failed")).isEqualTo(1);
    assertThat(result.get("errors").toString()).contains("unsupported").contains("https://example.com/remote.png");
  }

  @Test
  void markdownImportReportsUnsupportedLocalImageInsteadOfCreatingBrokenArticle() {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("source_type", "markdown");
    body.add(
        "files",
        resource(
            "local-image.md",
            """
            # Local Image Import

            ![Broken Local](images/local.png)
            """));

    ResponseEntity<Map> importResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles/import", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = (Map<?, ?>) data(importResponse);
    assertThat(result.get("success")).isEqualTo(0);
    assertThat(result.get("failed")).isEqualTo(1);
    assertThat(result.get("errors").toString()).contains("unsupported").contains("images/local.png");
  }

  @Test
  void commentImportReportsMissingTargetInsteadOfBindingToDefaultArticle() {
    HttpHeaders headers = authenticatedHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("source_type", "artalk");
    body.add(
        "file",
        resource(
            "missing-target-comments.json",
            """
            {
              "comments": [
                {
                  "content": "Comment without target",
                  "nick": "Importer",
                  "email": "importer@example.com"
                }
              ]
            }
            """));

    ResponseEntity<Map> importResponse =
        restTemplate.exchange(
            "/api/v1/admin/comments/import", HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> result = (Map<?, ?>) data(importResponse);
    assertThat(result.get("success")).isEqualTo(0);
    assertThat(result.get("failed")).isEqualTo(1);
    assertThat(result.get("errors").toString()).contains("target_key");
  }

  private Map<?, ?> adminArticleByTitle(String title) {
    ResponseEntity<Map> listResponse =
        restTemplate.exchange(
            "/api/v1/admin/articles?page_size=100", HttpMethod.GET, new HttpEntity<>(authenticatedHeaders()), Map.class);
    Map<?, ?> page = (Map<?, ?>) data(listResponse);
    return (Map<?, ?>)
        ((List<?>) page.get("list"))
            .stream()
                .filter(item -> title.equals(((Map<?, ?>) item).get("title")))
                .findFirst()
                .orElseThrow();
  }

  private List<ZipContent> zipContents(byte[] bytes) throws Exception {
    assertThat(bytes).isNotNull();
    java.util.ArrayList<ZipContent> entries = new java.util.ArrayList<>();
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zip.getNextEntry()) != null) {
        entries.add(new ZipContent(entry.getName(), zip.readAllBytes()));
      }
    }
    return entries;
  }

  private record ZipContent(String name, byte[] bytes) {}

  private ByteArrayResource resource(String filename, String content) {
    return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getFilename() {
        return filename;
      }
    };
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
