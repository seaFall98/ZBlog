package com.zblog.media.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.media.application.port.FileRepository;
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
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("filename", filename);
    params.put("originalName", originalName);
    params.put("fileUrl", fileUrl);
    params.put("fileType", fileType);
    params.put("fileSize", fileSize);
    params.put("uploadType", uploadType);
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

  public void markDeleted(long id) {
    fileMapper.markDeleted(id);
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
}
