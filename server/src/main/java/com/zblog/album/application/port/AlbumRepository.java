package com.zblog.album.application.port;

import com.zblog.common.api.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AlbumRepository {

  PageResponse<Map<String, Object>> listPublicRows(int page, int pageSize);

  Map<String, Object> findPublicBySlug(String slug);

  PageResponse<Map<String, Object>> listAdminRows(
      int page, int pageSize, String keyword, Boolean isPublic);

  Map<String, Object> findAlbum(long id);

  long createAlbum(
      String title,
      String slug,
      String description,
      String coverUrl,
      int sortOrder,
      boolean isPublic);

  void updateAlbum(
      long id,
      String title,
      String slug,
      String description,
      String coverUrl,
      int sortOrder,
      boolean isPublic);

  void deleteAlbum(long id);

  List<Map<String, Object>> listPhotoRows(long albumId, boolean publicOnly);

  Map<String, Object> findPhoto(long albumId, long photoId);

  long createPhoto(
      long albumId,
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt);

  void updatePhoto(
      long albumId,
      long photoId,
      Long fileId,
      String imageUrl,
      String title,
      String description,
      int sortOrder,
      boolean isPublic,
      Instant takenAt);

  void deletePhoto(long albumId, long photoId);

  void reorderPhotos(long albumId, List<Long> photoIds);
}
