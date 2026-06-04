package com.zblog.media.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.media.application.port.FileRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFileRepository implements FileRepository {

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
        or exists (
          select 1 from albums al
          where al.cover_url = f.file_url
        )
        or exists (
          select 1 from album_photos ap
          where ap.image_url = f.file_url
        )
      then 1 else 0 end
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcFileRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long create(
      String filename,
      String originalName,
      String fileUrl,
      String fileType,
      long fileSize,
      String uploadType) {
    return insertAndReturnId(
        """
        insert into files (filename, original_name, file_url, file_type, file_size, upload_type)
        values (?, ?, ?, ?, ?, ?)
        """,
        filename,
        originalName,
        fileUrl,
        fileType,
        fileSize,
        uploadType);
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
    if (start != null) {
      where.append(" and f.upload_time >= ? ");
      args.add(Timestamp.valueOf(start));
    }
    if (end != null) {
      where.append(" and f.upload_time < ? ");
      args.add(Timestamp.valueOf(end));
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

  public List<String> findActiveFilenames(long id) {
    return jdbcTemplate.query(
        "select filename from files where id = ? and deleted_at is null",
        (rs, rowNum) -> rs.getString("filename"),
        id);
  }

  public void markDeleted(long id) {
    jdbcTemplate.update("update files set deleted_at = current_timestamp where id = ?", id);
  }

  private long insertAndReturnId(String sql, Object... args) {
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
