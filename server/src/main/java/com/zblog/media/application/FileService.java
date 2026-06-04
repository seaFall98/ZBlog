package com.zblog.media.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.common.util.AdminDateRange;
import com.zblog.media.application.port.FileRepository;
import com.zblog.media.application.port.FileStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of(
          "image/jpeg",
          "image/jpg",
          "image/png",
          "image/gif",
          "image/webp",
          "image/avif",
          "image/bmp",
          "image/x-icon",
          "image/vnd.microsoft.icon",
          "application/pdf",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "text/plain");
  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

  private final FileRepository fileRepository;
  private final FileStorage fileStorage;

  public FileService(FileRepository fileRepository, FileStorage fileStorage) {
    this.fileRepository = fileRepository;
    this.fileStorage = fileStorage;
  }

  public Map<String, Object> upload(MultipartFile file, String type) throws IOException {
    validate(file);
    String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
    String safeOriginal = original.replaceAll("[^a-zA-Z0-9._-]", "_");
    String filename = Instant.now().toEpochMilli() + "_" + safeOriginal;
    String url;
    try (var content = file.getInputStream()) {
      url = fileStorage.store(filename, content);
    }

    long id =
        fileRepository.create(
            filename,
            original,
            url,
            file.getContentType() == null ? "" : file.getContentType(),
            file.getSize(),
            type);

    return Map.of(
        "id", id,
        "file_url", url,
        "file_name", filename,
        "original_name", original,
        "file_size", file.getSize());
  }

  private void validate(MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessException(40001, "文件不能为空", HttpStatus.BAD_REQUEST);
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessException(40001, "文件大小不能超过10MB", HttpStatus.BAD_REQUEST);
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BusinessException(40001, "不支持的文件类型", HttpStatus.BAD_REQUEST);
    }
  }

  public PageResponse<Map<String, Object>> list(int page, int pageSize) {
    return list(page, pageSize, null, null, null, null, null, null, null, null);
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
      String startTime,
      String endTime) {
    AdminDateRange dateRange = AdminDateRange.parse(startTime, endTime);
    return fileRepository.list(
        page,
        pageSize,
        keyword,
        fileType,
        status,
        uploadType,
        minSize,
        maxSize,
        dateRange.startInclusive(),
        dateRange.endExclusive());
  }

  public void delete(long id) {
    List<String> filenames = fileRepository.findActiveFilenames(id);
    fileRepository.markDeleted(id);
    if (!filenames.isEmpty()) {
      try {
        fileStorage.delete(filenames.getFirst());
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
  }

}
