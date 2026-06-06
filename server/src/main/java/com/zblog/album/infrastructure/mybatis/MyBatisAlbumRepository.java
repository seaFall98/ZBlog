package com.zblog.album.infrastructure.mybatis;

import com.zblog.album.application.port.AlbumRepository;
import com.zblog.common.api.PageResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisAlbumRepository implements AlbumRepository {

  private final AlbumMapper albumMapper;

  public MyBatisAlbumRepository(AlbumMapper albumMapper) {
    this.albumMapper = albumMapper;
  }

  public PageResponse<Map<String, Object>> listPublicRows(int page, int pageSize) {
    int offset = Math.max(0, page - 1) * pageSize;
    return new PageResponse<>(albumMapper.listPublicRows(pageSize, offset), albumMapper.countPublicAlbums(), page, pageSize);
  }

  public Map<String, Object> findPublicBySlug(String slug) {
    List<Map<String, Object>> rows = albumMapper.publicRowsBySlug(slug);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public PageResponse<Map<String, Object>> listAdminRows(int page, int pageSize, String keyword, Boolean isPublic) {
    int offset = Math.max(0, page - 1) * pageSize;
    String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
    return new PageResponse<>(
        albumMapper.listAdminRows(like, isPublic, pageSize, offset),
        albumMapper.countAdminRows(like, isPublic),
        page,
        pageSize);
  }

  public Map<String, Object> findAlbum(long id) {
    List<Map<String, Object>> rows = albumMapper.albumById(id);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public long createAlbum(
      String title, String slug, String description, String coverUrl, int sortOrder, boolean isPublic) {
    Map<String, Object> params = albumParams(0, title, slug, description, coverUrl, sortOrder, isPublic);
    albumMapper.insertAlbum(params);
    return generatedId(params);
  }

  public void updateAlbum(
      long id, String title, String slug, String description, String coverUrl, int sortOrder, boolean isPublic) {
    albumMapper.updateAlbum(albumParams(id, title, slug, description, coverUrl, sortOrder, isPublic));
  }

  public void deleteAlbum(long id) {
    albumMapper.deleteAlbum(id);
  }

  public List<Map<String, Object>> listPhotoRows(long albumId, boolean publicOnly) {
    return albumMapper.listPhotoRows(albumId, publicOnly);
  }

  public Map<String, Object> findPhoto(long albumId, long photoId) {
    List<Map<String, Object>> rows = albumMapper.photoById(albumId, photoId);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public long createPhoto(
      long albumId,
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt) {
    Map<String, Object> params = photoParams(albumId, 0, fileId, imageUrl, title, description, sortOrder, isPublic, takenAt);
    albumMapper.insertPhoto(params);
    return generatedId(params);
  }

  public void updatePhoto(
      long albumId,
      long photoId,
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt) {
    albumMapper.updatePhoto(photoParams(albumId, photoId, fileId, imageUrl, title, description, sortOrder, isPublic, takenAt));
  }

  public void deletePhoto(long albumId, long photoId) {
    albumMapper.deletePhoto(albumId, photoId);
  }

  public void reorderPhotos(long albumId, List<Long> photoIds) {
    int order = 1;
    for (Long photoId : photoIds) {
      albumMapper.updatePhotoSort(albumId, photoId, order++);
    }
  }

  private Map<String, Object> albumParams(
      long id, String title, String slug, String description, String coverUrl, int sortOrder, boolean isPublic) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("id", id);
    params.put("title", title);
    params.put("slug", slug);
    params.put("description", description);
    params.put("coverUrl", coverUrl);
    params.put("sortOrder", sortOrder);
    params.put("isPublic", isPublic);
    return params;
  }

  private Map<String, Object> photoParams(
      long albumId,
      long photoId,
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("albumId", albumId);
    params.put("photoId", photoId);
    params.put("fileId", fileId);
    params.put("imageUrl", imageUrl);
    params.put("title", title);
    params.put("description", description);
    params.put("sortOrder", sortOrder);
    params.put("isPublic", isPublic);
    params.put("takenAt", takenAt);
    return params;
  }

  private long generatedId(Map<String, Object> params) {
    return ((Number) params.get("id")).longValue();
  }
}
