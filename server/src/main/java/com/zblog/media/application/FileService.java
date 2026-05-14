package com.zblog.media.application;

import com.zblog.common.api.PageResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

  private final JdbcTemplate jdbcTemplate;
  private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();

  public FileService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Map<String, Object> upload(MultipartFile file, String type) throws IOException {
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
        "file_size", file.getSize());
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
    list.forEach(row -> row.put("upload_time", row.get("upload_time").toString()));
    return new PageResponse<>(list, list.size(), page, pageSize);
  }

  public void delete(long id) {
    jdbcTemplate.update("update files set deleted_at = current_timestamp where id = ?", id);
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
