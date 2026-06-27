package com.zblog.media.application.port;

import com.zblog.common.api.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FileRepository {

  long create(
      String filename,
      String originalName,
      String fileUrl,
      String fileType,
      long fileSize,
      String uploadType);

  default long create(
      String filename,
      String originalName,
      String fileUrl,
      String fileType,
      long fileSize,
      String uploadType,
      Long uploadedBy,
      String checksumSha256,
      FileStorageMetadata storageMetadata) {
    return create(filename, originalName, fileUrl, fileType, fileSize, uploadType);
  }

  PageResponse<Map<String, Object>> list(
      int page,
      int pageSize,
      String keyword,
      String fileType,
      Integer status,
      String uploadType,
      Long minSize,
      Long maxSize,
      LocalDateTime start,
      LocalDateTime end);

  List<FileStorageReference> findActiveStorageReferences(long id);

  default List<Map<String, Object>> findRecentUserUploadsByUrls(
      long userId, String uploadType, List<String> fileUrls) {
    return List.of();
  }

  default void bindFilesToComment(long userId, long commentId, String uploadType, List<String> fileUrls) {}

  default List<String> findActiveFilenames(long id) {
    return findActiveStorageReferences(id).stream().map(FileStorageReference::filename).toList();
  }

  void markDeleted(long id);
}
