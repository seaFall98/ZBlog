package com.zblog.media.application.port;

import com.zblog.common.api.PageResponse;
import java.time.LocalDateTime;
import java.util.Map;

public interface FileRepository {

  long create(
      String filename,
      String originalName,
      String fileUrl,
      String fileType,
      long fileSize,
      String uploadType);

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

  java.util.List<String> findActiveFilenames(long id);

  void markDeleted(long id);
}
