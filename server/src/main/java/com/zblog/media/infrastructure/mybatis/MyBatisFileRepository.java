package com.zblog.media.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.media.application.port.FileRepository;
import com.zblog.media.application.port.FileStorageMetadata;
import com.zblog.media.application.port.FileStorageReference;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisFileRepository implements FileRepository {

  private final FileMapper fileMapper;

  public MyBatisFileRepository(FileMapper fileMapper) {
    this.fileMapper = fileMapper;
  }

  public long create(
      String filename,
      String originalName,
      String fileUrl,
      String fileType,
      long fileSize,
      String uploadType) {
    return create(
        filename,
        originalName,
        fileUrl,
        fileType,
        fileSize,
        uploadType,
        null,
        "",
        FileStorageMetadata.empty());
  }

  public long create(
      String filename,
      String originalName,
      String fileUrl,
      String fileType,
      long fileSize,
      String uploadType,
      Long uploadedBy,
      String checksumSha256,
      FileStorageMetadata storageMetadata) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("filename", filename);
    params.put("originalName", originalName);
    params.put("fileUrl", fileUrl);
    params.put("fileType", fileType);
    params.put("fileSize", fileSize);
    params.put("uploadType", uploadType);
    params.put("uploadedBy", uploadedBy);
    params.put("checksumSha256", checksumSha256 == null ? "" : checksumSha256);
    FileStorageMetadata metadata = storageMetadata == null ? FileStorageMetadata.empty() : storageMetadata;
    params.put("storageProvider", metadata.provider());
    params.put("storageBucket", metadata.bucket());
    params.put("storageRegion", metadata.region());
    params.put("storageObjectKey", metadata.objectKey());
    params.put("storageDomain", metadata.domain());
    params.put("storagePrefix", metadata.prefix());
    fileMapper.insertFile(params);
    return ((Number) params.get("id")).longValue();
  }

  public PageResponse<Map<String, Object>> list(
      int page,
      int pageSize,
      String keyword,
      String fileType,
      Integer status,
      String uploadType,
      Long minSize,
      Long maxSize,
      LocalDateTime start,
      LocalDateTime end) {
    int offset = Math.max(0, page - 1) * pageSize;
    String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.toLowerCase() + "%";
    String fileTypeLike = fileTypeLike(fileType);
    long total =
        fileMapper.countRows(like, fileTypeLike, status, blankToNull(uploadType), minSize, maxSize, start, end);
    List<Map<String, Object>> rows =
        fileMapper.listRows(like, fileTypeLike, status, blankToNull(uploadType), minSize, maxSize, start, end, pageSize, offset);
    rows.forEach(
        row -> {
          row.put("upload_time", row.get("upload_time").toString());
          row.put("file_name", row.get("filename"));
        });
    return new PageResponse<>(rows, total, page, pageSize);
  }

  public List<String> findActiveFilenames(long id) {
    return fileMapper.activeFilenames(id);
  }

  @Override
  public List<FileStorageReference> findActiveStorageReferences(long id) {
    return fileMapper.activeStorageReferences(id).stream()
        .map(
            row ->
                new FileStorageReference(
                    string(row.get("filename")),
                    string(row.get("file_url")),
                    string(row.get("storage_provider")),
                    string(row.get("storage_bucket")),
                    string(row.get("storage_region")),
                    string(row.get("storage_object_key")),
                    string(row.get("storage_domain")),
                    string(row.get("storage_prefix"))))
        .toList();
  }

  public void markDeleted(long id) {
    fileMapper.markDeleted(id);
  }

  public List<Map<String, Object>> findRecentUserUploadsByUrls(
      long userId, String uploadType, List<String> fileUrls) {
    if (fileUrls.isEmpty()) {
      return List.of();
    }
    return fileMapper.recentUserUploadsByUrls(userId, uploadType, fileUrls);
  }

  public void bindFilesToComment(long userId, long commentId, String uploadType, List<String> fileUrls) {
    if (fileUrls.isEmpty()) {
      return;
    }
    fileMapper.bindFilesToComment(userId, commentId, uploadType, fileUrls);
  }

  private String fileTypeLike(String fileType) {
    if (fileType == null || fileType.isBlank()) {
      return null;
    }
    String lower = fileType.toLowerCase();
    return lower.contains("/") ? lower : lower + "/%";
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }
}
