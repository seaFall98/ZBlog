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
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Batch7FollowupOpsMediaTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void siteConfigPackageExportIncludesOnlyPortablePublicSiteConfiguration() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> originalPackage = exportSiteConfigPackage(headers);
    try {
      restTemplate.exchange(
          "/api/v1/admin/settings/v2_identity",
          HttpMethod.PUT,
          new HttpEntity<>(Map.of("site_title", "Batch7 Portable Site"), headers),
          Map.class);
      restTemplate.exchange(
          "/api/v1/admin/settings/ai",
          HttpMethod.PUT,
          new HttpEntity<>(Map.of("api_key", "fake-batch7-secret-never-export"), headers),
          Map.class);

      ResponseEntity<Map> response =
          restTemplate.exchange(
              "/api/v1/admin/site-config/package", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      Map<?, ?> exported = (Map<?, ?>) data(response);
      Map<?, ?> settings = (Map<?, ?>) exported.get("settings");
      Map<?, ?> identity = (Map<?, ?>) settings.get("v2_identity");

      assertThat(exported.get("version")).isEqualTo("zblog.site-config.v1");
      assertThat(identity.get("site_title")).isEqualTo("Batch7 Portable Site");
      List<String> settingGroups = settings.keySet().stream().map(Object::toString).toList();

      assertThat(settingGroups)
          .containsExactlyElementsOf(
              List.of("v2_identity", "v2_home", "v2_about", "v2_guestbook", "v2_footer", "v2_search"));
      assertThat(exported.toString()).doesNotContain("fake-batch7-secret-never-export", "api_key", "client_secret");
    } finally {
      importSiteConfigPackage(headers, originalPackage);
    }
  }

  @Test
  void siteConfigPackageImportReplacesPortableSettingsAndPublicMenus() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> originalPackage = exportSiteConfigPackage(headers);

    Map<String, Object> importedPackage =
        Map.of(
            "version",
            "zblog.site-config.v1",
            "settings",
            Map.of(
                "v2_identity", Map.of("site_title", "Imported Batch7 Site", "owner_display_name", "Batch7 Owner"),
                "v2_home", Map.of("hero_title", "Imported Hero"),
                "v2_about", Map.of("intro_text", "Imported About"),
                "v2_guestbook", Map.of("intro_text", "Imported Guestbook"),
                "v2_footer", Map.of("slogan", "Imported Footer"),
                "v2_search", Map.of("hot_keywords", "batch7,import")),
            "menus",
            Map.of(
                "header",
                List.of(
                    Map.of(
                        "title",
                        "Batch7 Home",
                        "url",
                        "/",
                        "icon",
                        "House",
                        "sort",
                        1,
                        "children",
                        List.of(Map.of("title", "Batch7 Gallery", "url", "/gallery", "sort", 2)))),
                "footer",
                List.of(Map.of("title", "Batch7 Links", "url", "/links", "sort", 1))));

    try {
      ResponseEntity<Map> importResponse =
          restTemplate.exchange(
              "/api/v1/admin/site-config/package",
              HttpMethod.PUT,
              new HttpEntity<>(importedPackage, headers),
              Map.class);
      assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

      Map<?, ?> frontConfig = (Map<?, ?>) data(restTemplate.getForEntity("/api/v1/front/config", Map.class));
      Map<?, ?> identity = (Map<?, ?>) frontConfig.get("identity");
      Map<?, ?> home = (Map<?, ?>) frontConfig.get("home");
      assertThat(identity.get("siteTitle")).isEqualTo("Imported Batch7 Site");
      assertThat(home.get("heroTitle")).isEqualTo("Imported Hero");

      Map<?, ?> frontMenus = (Map<?, ?>) data(restTemplate.getForEntity("/api/v1/front/menus", Map.class));
      List<?> header = (List<?>) frontMenus.get("header");
      Map<?, ?> importedHeader = (Map<?, ?>) header.getFirst();
      List<?> children = (List<?>) importedHeader.get("children");
      assertThat(importedHeader.get("title")).isEqualTo("Batch7 Home");
      assertThat(((Map<?, ?>) children.getFirst()).get("title")).isEqualTo("Batch7 Gallery");
      assertThat(importedHeader.get("id")).isNotEqualTo(((Map<?, ?>) children.getFirst()).get("id"));

      Map<?, ?> exported = exportSiteConfigPackage(headers);
      assertThat(exported.toString()).doesNotContain("api_key", "secret_key", "client_secret");
    } finally {
      importSiteConfigPackage(headers, originalPackage);
    }
  }

  @Test
  void siteConfigPackageImportRejectsIncompletePackageWithoutDeletingCurrentConfiguration() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> originalPackage = exportSiteConfigPackage(headers);
    try {
      restTemplate.exchange(
          "/api/v1/admin/settings/v2_identity",
          HttpMethod.PUT,
          new HttpEntity<>(Map.of("site_title", "Batch7 Existing Site"), headers),
          Map.class);

      ResponseEntity<Map> importResponse =
          restTemplate.exchange(
              "/api/v1/admin/site-config/package",
              HttpMethod.PUT,
              new HttpEntity<>(Map.of("version", "zblog.site-config.v1"), headers),
              Map.class);

      assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

      Map<?, ?> frontConfig = (Map<?, ?>) data(restTemplate.getForEntity("/api/v1/front/config", Map.class));
      Map<?, ?> identity = (Map<?, ?>) frontConfig.get("identity");
      assertThat(identity.get("siteTitle")).isEqualTo("Batch7 Existing Site");
    } finally {
      importSiteConfigPackage(headers, originalPackage);
    }
  }

  @Test
  void siteConfigPackageImportRejectsMissingMenusWithoutDeletingCurrentConfiguration() {
    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> originalPackage = exportSiteConfigPackage(headers);
    try {
      restTemplate.exchange(
          "/api/v1/admin/settings/v2_identity",
          HttpMethod.PUT,
          new HttpEntity<>(Map.of("site_title", "Batch7 Existing Site Before Bad Menus"), headers),
          Map.class);

      ResponseEntity<Map> importResponse =
          restTemplate.exchange(
              "/api/v1/admin/site-config/package",
              HttpMethod.PUT,
              new HttpEntity<>(
                  Map.of(
                      "version",
                      "zblog.site-config.v1",
                      "settings",
                      Map.of(
                          "v2_identity", Map.of("site_title", "Should Not Persist"),
                          "v2_home", Map.of(),
                          "v2_about", Map.of(),
                          "v2_guestbook", Map.of(),
                          "v2_footer", Map.of(),
                          "v2_search", Map.of())),
                  headers),
              Map.class);

      assertThat(importResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

      Map<?, ?> frontConfig = (Map<?, ?>) data(restTemplate.getForEntity("/api/v1/front/config", Map.class));
      Map<?, ?> identity = (Map<?, ?>) frontConfig.get("identity");
      assertThat(identity.get("siteTitle")).isEqualTo("Batch7 Existing Site Before Bad Menus");
    } finally {
      importSiteConfigPackage(headers, originalPackage);
    }
  }

  @Test
  void adminNotificationCenterFiltersAndProcessesOperationalNotifications() {
    ResponseEntity<Map> feedbackResponse =
        restTemplate.postForEntity(
            "/api/v1/feedback",
            Map.of(
                "reportUrl", "/posts/batch7-ops",
                "reportType", "suggestion",
                "email", "batch7-ops@example.com",
                "description", "Batch7 notification center keyword"),
            Map.class);
    assertThat(feedbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    HttpHeaders headers = authenticatedHeaders();
    Map<?, ?> page =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/notifications?page=1&page_size=10&type=feedback_new&read=false&processed=false&keyword=Batch7",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class));
    List<?> list = (List<?>) page.get("list");
    assertThat(list).isNotEmpty();
    Map<?, ?> first = (Map<?, ?>) list.getFirst();
    assertThat(first.get("type")).isEqualTo("feedback_new");
    assertThat(first.get("is_read")).isEqualTo(false);
    assertThat(first.get("is_processed")).isEqualTo(false);

    Number id = (Number) first.get("id");
    Map<?, ?> processed =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/notifications/" + id + "/processed",
                    HttpMethod.PUT,
                    new HttpEntity<>(headers),
                    Map.class));
    assertThat(processed.get("is_processed")).isEqualTo(true);
    assertThat(processed.get("processed_at")).isNotNull();

    Map<?, ?> processedPage =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/notifications?page=1&page_size=10&type=feedback_new&processed=true&keyword=Batch7",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class));
    assertThat((List<?>) processedPage.get("list")).anySatisfy(row -> assertThat(((Map<?, ?>) row).get("id")).isEqualTo(id));

    Map<?, ?> unprocessed =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/notifications/" + id + "/unprocessed",
                    HttpMethod.PUT,
                    new HttpEntity<>(headers),
                    Map.class));
    assertThat(unprocessed.get("is_processed")).isEqualTo(false);
    assertThat(unprocessed.get("processed_at")).isNull();
  }

  @Test
  void mediaStorageSettingsDoNotPersistSecretsAndLocalUploadsStillWork() {
    HttpHeaders headers = authenticatedHeaders();
    restTemplate.exchange(
        "/api/v1/admin/settings/upload",
        HttpMethod.PUT,
        new HttpEntity<>(
            Map.of(
                "upload.storage_type", "local",
                "upload.region", "ap-guangzhou",
                "upload.bucket", "zblog-bucket",
                "upload.domain", "https://cdn.example.com",
                "upload.secret_id", "AKID-batch7-secret-id",
                "upload.secret_key", "batch7-secret-key"),
            headers),
        Map.class);

    Map<?, ?> uploadSettings =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/settings/upload", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
    assertThat(uploadSettings.toString()).doesNotContain("AKID-batch7-secret-id", "batch7-secret-key", "secret_key");

    Map<?, ?> status =
        (Map<?, ?>)
            data(
                restTemplate.exchange(
                    "/api/v1/admin/media/storage/status", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
    assertThat(status.get("storage_type")).isEqualTo("local");
    assertThat(status.get("credential_configured")).isEqualTo(false);

    HttpHeaders uploadHeaders = new HttpHeaders();
    uploadHeaders.setBearerAuth(headers.getFirst(HttpHeaders.AUTHORIZATION).replace("Bearer ", ""));
    uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file",
        new ByteArrayResource("batch7 upload".getBytes()) {
          @Override
          public String getFilename() {
            return "batch7-upload.txt";
          }
        });
    ResponseEntity<Map> uploadResponse =
        restTemplate.exchange(
            "/api/v1/admin/files?type=batch7",
            HttpMethod.POST,
            new HttpEntity<>(body, uploadHeaders),
            Map.class);
    Map<?, ?> upload = (Map<?, ?>) data(uploadResponse);
    assertThat(upload.get("file_url").toString()).startsWith("/uploads/");
  }

  private HttpHeaders authenticatedHeaders() {
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "/api/v1/auth/login", Map.of("username", "admin", "password", "admin123456"), Map.class);
    String token = ((Map<?, ?>) data(response)).get("access_token").toString();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Object data(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("code")).isEqualTo(0);
    return body.get("data");
  }

  private Map<?, ?> exportSiteConfigPackage(HttpHeaders headers) {
    return (Map<?, ?>)
        data(
            restTemplate.exchange(
                "/api/v1/admin/site-config/package", HttpMethod.GET, new HttpEntity<>(headers), Map.class));
  }

  private void importSiteConfigPackage(HttpHeaders headers, Map<?, ?> siteConfigPackage) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/site-config/package",
            HttpMethod.PUT,
            new HttpEntity<>(siteConfigPackage, headers),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
