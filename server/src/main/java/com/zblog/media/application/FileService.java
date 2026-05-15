package com.zblog.media.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
          "application/pdf",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "text/plain");
  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

  private final JdbcTemplate jdbcTemplate;
  private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();

  public FileService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> upload(MultipartFile file, String type) throws IOException {
    validate(file);
    Files.createDirectories(uploadRoot);
    String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
    String safeOriginal = original.replaceAll("[^a-zA-Z0-9._-]", "_");
    String filename = Instant.now().toEpochMilli() + "_" + safeOriginal;
    Path target = uploadRoot.resolve(filename).normalize();
    file.transferTo(target);
    String url = "/uploads/" + filename;

    long id =
        insertAndReturnId(
            """
            insert into files (filename, original_name, file_url, file_type, file_size, upload_type)
            values (?, ?, ?, ?, ?, ?)
            """,
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
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select id, filename, original_name, file_url, file_type, file_size,
              upload_type, upload_time, status
            from files
            where deleted_at is null
            order by upload_time desc, id desc
            """);
    list.forEach(
        row -> {
          row.put("upload_time", row.get("upload_time").toString());
          row.put("file_name", row.get("filename"));
        });
    return new PageResponse<>(list, list.size(), page, pageSize);
  }

  public void delete(long id) {
    List<String> filenames =
        jdbcTemplate.query(
            "select filename from files where id = ? and deleted_at is null",
            (rs, rowNum) -> rs.getString("filename"),
            id);
    jdbcTemplate.update("update files set deleted_at = current_timestamp where id = ?", id);
    if (!filenames.isEmpty()) {
      try {
        Path target = uploadRoot.resolve(filenames.getFirst()).normalize();
        if (target.startsWith(uploadRoot)) {
          Files.deleteIfExists(target);
        }
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
  }

  long insertAndReturnId(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);
    Map<String, Object> keys = keyHolder.getKeys();
    if (keys != null && keys.get("id") instanceof Number number) {
      return number.longValue();
    }
    return keyHolder.getKey().longValue();
  }
}
