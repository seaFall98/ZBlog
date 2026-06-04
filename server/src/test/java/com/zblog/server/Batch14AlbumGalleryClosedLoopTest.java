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
class Batch14AlbumGalleryClosedLoopTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void adminCreatesPublicAlbumAndPublicListReadsIt() {
    Map<?, ?> album = createAlbum("Batch14 Public Album", "batch14-public-album", true, 10);

    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/albums?page_size=100", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<?> albums = list(data(response));
    assertThat(albums)
        .anySatisfy(
            item -> {
              Map<?, ?> row = (Map<?, ?>) item;
              assertThat(row.get("id")).isEqualTo(album.get("id"));
              assertThat(row.get("title")).isEqualTo("Batch14 Public Album");
              assertThat(row.get("slug")).isEqualTo("batch14-public-album");
            });
  }

  @Test
  void privateAlbumDoesNotAppearInPublicList() {
    Map<?, ?> album = createAlbum("Batch14 Private Album", "batch14-private-album", false, 20);

    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/albums?page_size=100", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list(data(response))).noneSatisfy(item -> assertThat(((Map<?, ?>) item).get("id")).isEqualTo(album.get("id")));
  }

  @Test
  void publicAlbumDetailReadsOnlyPublicPhotos() {
    Map<?, ?> album = createAlbum("Batch14 Photo Album", "batch14-photo-album", true, 30);
    Number albumId = (Number) album.get("id");
    Map<?, ?> publicPhoto = addPhoto(albumId, "/uploads/batch14-photo-a.png", "Visible photo", true, 2);
    Map<?, ?> privatePhoto = addPhoto(albumId, "/uploads/batch14-photo-b.png", "Hidden photo", false, 1);

    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/albums/batch14-photo-album", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> detail = (Map<?, ?>) data(response);
    assertThat(detail.get("slug")).isEqualTo("batch14-photo-album");
    assertThat(detail.get("photos"))
        .asList()
        .anySatisfy(item -> assertThat(((Map<?, ?>) item).get("id")).isEqualTo(publicPhoto.get("id")))
        .noneSatisfy(item -> assertThat(((Map<?, ?>) item).get("id")).isEqualTo(privatePhoto.get("id")));
  }

  @Test
  void photoOrderPersistsAcrossPublicReads() {
    Map<?, ?> album = createAlbum("Batch14 Sort Album", "batch14-sort-album", true, 40);
    Number albumId = (Number) album.get("id");
    Map<?, ?> second = addPhoto(albumId, "/uploads/batch14-second.png", "Second", true, 2);
    Map<?, ?> first = addPhoto(albumId, "/uploads/batch14-first.png", "First", true, 1);

    restTemplate.exchange(
        "/api/v1/admin/albums/" + albumId + "/photos/reorder",
        HttpMethod.PUT,
        new HttpEntity<>(
            Map.of(
                "photo_ids",
                List.of(first.get("id"), second.get("id"))),
            authenticatedHeaders()),
        Map.class);

    Map<?, ?> detail = (Map<?, ?>) data(restTemplate.getForEntity("/api/v1/albums/batch14-sort-album", Map.class));
    List<?> photos = (List<?>) detail.get("photos");
    assertThat(((Map<?, ?>) photos.get(0)).get("id")).isEqualTo(first.get("id"));
    assertThat(((Map<?, ?>) photos.get(1)).get("id")).isEqualTo(second.get("id"));
  }

  @Test
  void deletingPhotoAndAlbumRemovesThemFromPublicApi() {
    Map<?, ?> album = createAlbum("Batch14 Delete Album", "batch14-delete-album", true, 50);
    Number albumId = (Number) album.get("id");
    Map<?, ?> photo = addPhoto(albumId, "/uploads/batch14-delete.png", "Delete me", true, 1);

    ResponseEntity<Map> photoDelete =
        restTemplate.exchange(
            "/api/v1/admin/albums/" + albumId + "/photos/" + photo.get("id"),
            HttpMethod.DELETE,
            new HttpEntity<>(authenticatedHeaders()),
            Map.class);
    assertThat(photoDelete.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> afterPhotoDelete = (Map<?, ?>) data(restTemplate.getForEntity("/api/v1/albums/batch14-delete-album", Map.class));
    assertThat((List<?>) afterPhotoDelete.get("photos")).isEmpty();

    ResponseEntity<Map> albumDelete =
        restTemplate.exchange(
            "/api/v1/admin/albums/" + albumId,
            HttpMethod.DELETE,
            new HttpEntity<>(authenticatedHeaders()),
            Map.class);
    assertThat(albumDelete.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(restTemplate.getForEntity("/api/v1/albums/batch14-delete-album", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void duplicateSlugReturnsClearError() {
    createAlbum("Batch14 Slug A", "batch14-duplicate-slug", true, 60);

    ResponseEntity<Map> duplicate =
        restTemplate.exchange(
            "/api/v1/admin/albums",
            HttpMethod.POST,
            new HttpEntity<>(albumPayload("Batch14 Slug B", "batch14-duplicate-slug", true, 61), authenticatedHeaders()),
            Map.class);

    assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(duplicate.getBody()).containsEntry("code", 40001);
    assertThat(duplicate.getBody().get("message")).asString().contains("slug");
  }

  @Test
  void publicNavigationContainsAlbumEntry() {
    ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/menus", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(response))
        .asList()
        .anySatisfy(
            item -> {
              Map<?, ?> menu = (Map<?, ?>) item;
              assertThat(menu.get("type")).isEqualTo("navigation");
              assertThat(menu.get("title")).isEqualTo("相册");
              assertThat(menu.get("url")).isEqualTo("/album");
            });
  }

  @Test
  void photoUrlUsesRealUploadsUrl() {
    Map<?, ?> album = createAlbum("Batch14 Upload Url Album", "batch14-upload-url-album", true, 70);
    Map<?, ?> photo = addPhoto((Number) album.get("id"), "/uploads/batch14-real-url.png", "Real URL", true, 1);

    assertThat(photo.get("image_url")).isEqualTo("/uploads/batch14-real-url.png");
    Map<?, ?> detail = (Map<?, ?>) data(restTemplate.getForEntity("/api/v1/albums/batch14-upload-url-album", Map.class));
    assertThat(detail.get("photos")).asList().anySatisfy(item -> assertThat(((Map<?, ?>) item).get("image_url")).isEqualTo("/uploads/batch14-real-url.png"));
  }

  private Map<?, ?> createAlbum(String title, String slug, boolean isPublic, int sortOrder) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/albums",
            HttpMethod.POST,
            new HttpEntity<>(albumPayload(title, slug, isPublic, sortOrder), authenticatedHeaders()),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (Map<?, ?>) data(response);
  }

  private Map<String, Object> albumPayload(String title, String slug, boolean isPublic, int sortOrder) {
    return Map.of(
        "title", title,
        "slug", slug,
        "description", "Batch14 album description",
        "cover_url", "/uploads/" + slug + "-cover.png",
        "sort_order", sortOrder,
        "is_public", isPublic);
  }

  private Map<?, ?> addPhoto(Number albumId, String imageUrl, String title, boolean isPublic, int sortOrder) {
    ResponseEntity<Map> response =
        restTemplate.exchange(
            "/api/v1/admin/albums/" + albumId + "/photos",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "image_url", imageUrl,
                    "title", title,
                    "description", title + " description",
                    "sort_order", sortOrder,
                    "is_public", isPublic,
                    "taken_at", "2026-05-19T12:00:00Z"),
                authenticatedHeaders()),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (Map<?, ?>) data(response);
  }

  private List<?> list(Object page) {
    return (List<?>) ((Map<?, ?>) page).get("list");
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
