package com.zblog.media.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.common.util.AdminDateRange;
import com.zblog.media.application.port.FileRepository;
import com.zblog.media.application.port.FileStorage;
import com.zblog.media.application.port.FileStorageReference;
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
  private static final long MAX_AVATAR_FILE_SIZE = 2L * 1024 * 1024;
  private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES =
      Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

  private final FileRepository fileRepository;
  private final FileStorage fileStorage;

  public FileService(FileRepository fileRepository, FileStorage fileStorage) {
    this.fileRepository = fileRepository;
    this.fileStorage = fileStorage;
  }

  public Map<String, Object> upload(MultipartFile file, String type) throws IOException {
    validate(file, type);
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

  private void validate(MultipartFile file, String type) {
    if (file.isEmpty()) {
      throw new BusinessException(40001, "文件不能为空", HttpStatus.BAD_REQUEST);
    }
    String contentType = file.getContentType();
    if ("用户头像".equals(type)) {
      if (file.getSize() > MAX_AVATAR_FILE_SIZE) {
        throw new BusinessException(40001, "头像大小不能超过2MB", HttpStatus.BAD_REQUEST);
      }
      if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType)) {
        throw new BusinessException(40001, "头像仅支持 jpg、jpeg、png、webp、gif", HttpStatus.BAD_REQUEST);
      }
      // Verify magic bytes so Content-Type spoofing does not bypass the check.
      if (!hasImageMagicBytes(file)) {
        throw new BusinessException(40001, "头像仅支持 jpg、jpeg、png、webp、gif", HttpStatus.BAD_REQUEST);
      }
      return;
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessException(40001, "文件大小不能超过10MB", HttpStatus.BAD_REQUEST);
    }
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BusinessException(40001, "不支持的文件类型", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean hasImageMagicBytes(MultipartFile file) {
    try {
      byte[] header = file.getBytes();
      if (header.length < 4) return false;
      // JPEG: FF D8 FF
      if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return true;
      // PNG: 89 50 4E 47
      if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return true;
      // GIF: 47 49 46 38
      if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) return true;
      // WebP: 52 49 46 46 ... 57 45 42 50 at offset 8
      if (header.length >= 12
          && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
          && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) return true;
      return false;
    } catch (IOException e) {
      return false;
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
    List<FileStorageReference> references = fileRepository.findActiveStorageReferences(id);
    if (!references.isEmpty()) {
      try {
        FileStorageReference reference = references.getFirst();
        fileStorage.delete(reference.filename(), reference.fileUrl());
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
    fileRepository.markDeleted(id);
  }

}
