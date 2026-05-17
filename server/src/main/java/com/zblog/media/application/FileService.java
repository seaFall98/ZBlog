package com.zblog.media.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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

  private static final String FILE_USED_STATUS_SQL =
      """
      case when f.status = 1
        or exists (
          select 1 from articles a
          where a.cover_url = f.file_url
            or a.content_markdown like concat('%', f.file_url, '%')
            or a.content_html like concat('%', f.file_url, '%')
        )
        or exists (
          select 1 from settings s
          where s.value_text like concat('%', f.file_url, '%')
        )
        or exists (
          select 1 from comments c
          where c.content like concat('%', f.file_url, '%')
            or c.avatar = f.file_url
        )
        or exists (
          select 1 from feedbacks fb
          where fb.form_content like concat('%', f.file_url, '%')
            or fb.admin_reply like concat('%', f.file_url, '%')
        )
        or exists (
          select 1 from users u
          where u.avatar = f.file_url
        )
        or exists (
          select 1 from friends fr
          where fr.avatar = f.file_url
            or fr.screenshot = f.file_url
        )
        or exists (
          select 1 from moments m
          where m.content_json like concat('%', f.file_url, '%')
        )
      then 1 else 0 end
      """;

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
    int offset = Math.max(0, page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where f.deleted_at is null ");
    if (keyword != null && !keyword.isBlank()) {
      where.append(" and (lower(f.filename) like ? or lower(f.original_name) like ?) ");
      String like = "%" + keyword.toLowerCase() + "%";
      args.add(like);
      args.add(like);
    }
    if (fileType != null && !fileType.isBlank()) {
      where.append(" and lower(f.file_type) like ? ");
      args.add(fileType.toLowerCase().contains("/") ? fileType.toLowerCase() : fileType.toLowerCase() + "/%");
    }
    if (status != null) {
      where.append(" and ").append(FILE_USED_STATUS_SQL).append(" = ? ");
      args.add(status);
    }
    if (uploadType != null && !uploadType.isBlank()) {
      where.append(" and f.upload_type = ? ");
      args.add(uploadType);
    }
    if (minSize != null) {
      where.append(" and f.file_size >= ? ");
      args.add(minSize);
    }
    if (maxSize != null) {
      where.append(" and f.file_size <= ? ");
      args.add(maxSize);
    }
    LocalDate start = parseNullableDate(startTime);
    LocalDate end = parseNullableDate(endTime);
    if (start != null) {
      where.append(" and f.upload_time >= ? ");
      args.add(Timestamp.valueOf(start.atStartOfDay()));
    }
    if (end != null) {
      where.append(" and f.upload_time < ? ");
      args.add(Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
    }
    Long total = jdbcTemplate.queryForObject("select count(*) from files f" + where, Long.class, args.toArray());
    args.add(pageSize);
    args.add(offset);
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select f.id, f.filename, f.original_name, f.file_url, f.file_type, f.file_size,
              f.upload_type, f.upload_time,
            """
                + FILE_USED_STATUS_SQL
                + """
              as status
            from files f
            """
                + where
                + """
            order by f.upload_time desc, f.id desc
            limit ? offset ?
            """,
            args.toArray());
    list.forEach(
        row -> {
          row.put("upload_time", row.get("upload_time").toString());
          row.put("file_name", row.get("filename"));
        });
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
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

  private LocalDate parseNullableDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalDate.parse(value);
  }
}
