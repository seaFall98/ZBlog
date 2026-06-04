package com.zblog.album.application;

import com.zblog.album.application.port.AlbumRepository;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlbumService {

  private final AlbumRepository albumRepository;

  public AlbumService(AlbumRepository albumRepository) {
    this.albumRepository = albumRepository;
  }

  public PageResponse<Map<String, Object>> listPublic(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    PageResponse<Map<String, Object>> rows = albumRepository.listPublicRows(normalizedPage, normalizedPageSize);
    List<Map<String, Object>> list = rows.list().stream().map(this::albumView).toList();
    return new PageResponse<>(list, rows.total(), rows.page(), rows.pageSize());
  }

  public Map<String, Object> getPublicBySlug(String slug) {
    Map<String, Object> album = findPublicBySlug(slug);
    Map<String, Object> view = albumView(album);
    view.put("photos", listPhotos(number(album.get("id")).longValue(), true));
    return view;
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize, String keyword, Boolean isPublic) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    PageResponse<Map<String, Object>> rows =
        albumRepository.listAdminRows(normalizedPage, normalizedPageSize, keyword, isPublic);
    List<Map<String, Object>> list = rows.list().stream().map(this::albumView).toList();
    return new PageResponse<>(list, rows.total(), rows.page(), rows.pageSize());
  }

  public Map<String, Object> getAdmin(long id) {
    Map<String, Object> album = findAlbum(id);
    Map<String, Object> view = albumView(album);
    view.put("photos", listPhotos(id, false));
    return view;
  }

  @Transactional
  public Map<String, Object> create(Map<String, Object> request) {
    AlbumInput input = albumInput(request, null);
    try {
      long albumId =
          albumRepository.createAlbum(
              input.title(),
              input.slug(),
              input.description(),
              input.coverUrl(),
              input.sortOrder(),
              input.isPublic());
      return getAdmin(albumId);
    } catch (DuplicateKeyException exception) {
      throw new BusinessException(40001, "album slug already exists", HttpStatus.BAD_REQUEST);
    }
  }

  @Transactional
  public Map<String, Object> update(long id, Map<String, Object> request) {
    Map<String, Object> existing = findAlbum(id);
    AlbumInput input = albumInput(request, existing);
    try {
      albumRepository.updateAlbum(
          id,
          input.title(),
          input.slug(),
          input.description(),
          input.coverUrl(),
          input.sortOrder(),
          input.isPublic());
    } catch (DuplicateKeyException exception) {
      throw new BusinessException(40001, "album slug already exists", HttpStatus.BAD_REQUEST);
    }
    return getAdmin(id);
  }

  @Transactional
  public void delete(long id) {
    findAlbum(id);
    albumRepository.deleteAlbum(id);
  }

  @Transactional
  public Map<String, Object> addPhoto(long albumId, Map<String, Object> request) {
    findAlbum(albumId);
    PhotoInput input = photoInput(request, null);
    long photoId =
        albumRepository.createPhoto(
            albumId,
            input.fileId(),
            input.imageUrl(),
            input.title(),
            input.description(),
            input.sortOrder(),
            input.isPublic(),
            input.takenAt());
    return photoView(findPhoto(albumId, photoId));
  }

  @Transactional
  public Map<String, Object> updatePhoto(long albumId, long photoId, Map<String, Object> request) {
    Map<String, Object> existing = findPhoto(albumId, photoId);
    PhotoInput input = photoInput(request, existing);
    albumRepository.updatePhoto(
        albumId,
        photoId,
        input.fileId(),
        input.imageUrl(),
        input.title(),
        input.description(),
        input.sortOrder(),
        input.isPublic(),
        input.takenAt());
    return photoView(findPhoto(albumId, photoId));
  }

  @Transactional
  public void deletePhoto(long albumId, long photoId) {
    findPhoto(albumId, photoId);
    albumRepository.deletePhoto(albumId, photoId);
  }

  @Transactional
  public List<Map<String, Object>> reorderPhotos(long albumId, Map<String, Object> request) {
    findAlbum(albumId);
    Object ids = request.get("photo_ids");
    if (!(ids instanceof Iterable<?> iterable)) {
      throw new BusinessException(40001, "photo_ids is required", HttpStatus.BAD_REQUEST);
    }
    List<Long> photoIds = new ArrayList<>();
    for (Object id : iterable) {
      photoIds.add(number(id).longValue());
    }
    albumRepository.reorderPhotos(albumId, photoIds);
    return listPhotos(albumId, false);
  }

  private Map<String, Object> findPublicBySlug(String slug) {
    Map<String, Object> row = albumRepository.findPublicBySlug(slug);
    if (row == null) {
      throw new BusinessException(404, "Album not found", HttpStatus.NOT_FOUND);
    }
    return row;
  }

  private Map<String, Object> findAlbum(long id) {
    Map<String, Object> row = albumRepository.findAlbum(id);
    if (row == null) {
      throw new BusinessException(404, "Album not found", HttpStatus.NOT_FOUND);
    }
    return row;
  }

  private Map<String, Object> findPhoto(long albumId, long photoId) {
    Map<String, Object> row = albumRepository.findPhoto(albumId, photoId);
    if (row == null) {
      throw new BusinessException(404, "Album photo not found", HttpStatus.NOT_FOUND);
    }
    return row;
  }

  private List<Map<String, Object>> listPhotos(long albumId, boolean publicOnly) {
    return albumRepository.listPhotoRows(albumId, publicOnly).stream().map(this::photoView).toList();
  }

  private Map<String, Object> albumView(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", row.get("id"));
    view.put("title", row.get("title"));
    view.put("slug", row.get("slug"));
    view.put("description", row.get("description"));
    view.put("cover_url", row.get("cover_url"));
    view.put("sort_order", row.get("sort_order"));
    view.put("is_public", row.get("is_public"));
    view.put("photo_count", row.getOrDefault("photo_count", 0));
    view.put("created_at", stringTime(row.get("created_at")));
    view.put("updated_at", stringTime(row.get("updated_at")));
    return view;
  }

  private Map<String, Object> photoView(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", row.get("id"));
    view.put("album_id", row.get("album_id"));
    view.put("file_id", row.get("file_id"));
    view.put("image_url", row.get("image_url"));
    view.put("title", row.get("title"));
    view.put("description", row.get("description"));
    view.put("sort_order", row.get("sort_order"));
    view.put("is_public", row.get("is_public"));
    view.put("taken_at", stringTime(row.get("taken_at")));
    view.put("created_at", stringTime(row.get("created_at")));
    view.put("updated_at", stringTime(row.get("updated_at")));
    return view;
  }

  private AlbumInput albumInput(Map<String, Object> request, Map<String, Object> existing) {
    String title = stringValue(value(request, existing, "title"));
    String slug = stringValue(value(request, existing, "slug"));
    if (title.isBlank()) {
      throw new BusinessException(40001, "title is required", HttpStatus.BAD_REQUEST);
    }
    if (slug.isBlank()) {
      throw new BusinessException(40001, "slug is required", HttpStatus.BAD_REQUEST);
    }
    if (!slug.matches("[a-z0-9][a-z0-9-]{0,158}[a-z0-9]")) {
      throw new BusinessException(40001, "slug format is invalid", HttpStatus.BAD_REQUEST);
    }
    return new AlbumInput(
        title,
        slug,
        nullableString(value(request, existing, "description")),
        nullableString(value(request, existing, "cover_url")),
        integerValue(value(request, existing, "sort_order"), 0),
        booleanValue(value(request, existing, "is_public"), true));
  }

  private PhotoInput photoInput(Map<String, Object> request, Map<String, Object> existing) {
    String imageUrl = stringValue(value(request, existing, "image_url"));
    if (imageUrl.isBlank()) {
      throw new BusinessException(40001, "image_url is required", HttpStatus.BAD_REQUEST);
    }
    if (!imageUrl.startsWith("/uploads/") && !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
      throw new BusinessException(40001, "image_url must be an uploads or remote URL", HttpStatus.BAD_REQUEST);
    }
    return new PhotoInput(
        nullableLong(value(request, existing, "file_id")),
        imageUrl,
        nullableString(value(request, existing, "title")),
        nullableString(value(request, existing, "description")),
        integerValue(value(request, existing, "sort_order"), 0),
        booleanValue(value(request, existing, "is_public"), true),
        timestamp(value(request, existing, "taken_at")));
  }

  private Object value(Map<String, Object> request, Map<String, Object> existing, String key) {
    if (request.containsKey(key)) {
      return request.get(key);
    }
    return existing == null ? null : existing.get(key);
  }

  private String nullableString(Object value) {
    String text = stringValue(value);
    return text.isBlank() ? null : text;
  }

  private String stringValue(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  private Integer integerValue(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      return Integer.parseInt(text);
    }
    return fallback;
  }

  private Long nullableLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text && !text.isBlank()) {
      return Long.parseLong(text);
    }
    return null;
  }

  private boolean booleanValue(Object value, boolean fallback) {
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof String text && !text.isBlank()) {
      return Boolean.parseBoolean(text);
    }
    return fallback;
  }

  private Number number(Object value) {
    if (value instanceof Number number) {
      return number;
    }
    if (value instanceof String text && !text.isBlank()) {
      return Long.parseLong(text);
    }
    throw new BusinessException(40001, "invalid id", HttpStatus.BAD_REQUEST);
  }

  private Instant timestamp(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return OffsetDateTime.parse(text).toInstant();
      } catch (DateTimeParseException ignored) {
        return LocalDateTime.parse(text.replace(' ', 'T'))
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant();
      }
    }
    return null;
  }

  private String stringTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant instant) {
      return instant.toString();
    }
    if (value instanceof java.util.Date date) {
      return date.toInstant().toString();
    }
    return value.toString();
  }

  private record AlbumInput(
      String title, String slug, String description, String coverUrl, int sortOrder, boolean isPublic) {}

  private record PhotoInput(
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt) {}
}
