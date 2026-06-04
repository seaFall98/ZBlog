package com.zblog.comment.infrastructure;

import com.zblog.comment.application.port.CommentRepository;
import com.zblog.common.api.PageResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcCommentRepository implements CommentRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcCommentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> listPublicRows(String targetType, String targetKey) {
    return jdbcTemplate.queryForList(
        """
        select * from comments
        where target_type = ? and target_key = ? and status = 1 and is_deleted = false
        order by created_at desc, id desc
        """,
        targetType,
        targetKey);
  }

  public PageResponse<Map<String, Object>> listAdminRows(
      int page,
      int pageSize,
      String keyword,
      Integer status,
      Boolean deleted,
      Boolean sub,
      LocalDateTime start,
      LocalDateTime end) {
    int offset = Math.max(0, page - 1) * pageSize;
    List<Object> args = new ArrayList<>();
    StringBuilder where = new StringBuilder(" where 1 = 1");
    if (keyword != null && !keyword.isBlank()) {
      where.append(
          " and (lower(content) like ? or lower(nickname) like ? or lower(email) like ? or lower(target_key) like ?)");
      String like = "%" + keyword.toLowerCase() + "%";
      args.add(like);
      args.add(like);
      args.add(like);
      args.add(like);
    }
    if (status != null) {
      where.append(" and status = ?");
      args.add(status);
    }
    if (deleted != null) {
      where.append(" and is_deleted = ?");
      args.add(deleted);
    }
    if (sub != null) {
      where.append(sub ? " and parent_id is not null" : " and parent_id is null");
    }
    if (start != null) {
      where.append(" and created_at >= ?");
      args.add(Timestamp.valueOf(start));
    }
    if (end != null) {
      where.append(" and created_at < ?");
      args.add(Timestamp.valueOf(end));
    }

    Long total =
        jdbcTemplate.queryForObject("select count(*) from comments" + where, Long.class, args.toArray());
    args.add(pageSize);
    args.add(offset);
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            "select * from comments" + where + " order by created_at desc, id desc limit ? offset ?",
            args.toArray());
    return new PageResponse<>(list, total == null ? 0 : total, page, pageSize);
  }

  public long create(
      String targetType,
      String targetKey,
      Long parentId,
      String content,
      String nickname,
      String email,
      String website,
      String avatar) {
    return insertAndReturnId(
        """
        insert into comments (
          target_type, target_key, parent_id, content, nickname, email, website, avatar
        ) values (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        targetType,
        targetKey,
        parentId,
        content,
        nickname,
        email,
        website,
        avatar);
  }

  public long importComment(
      String targetType,
      String targetKey,
      Long parentId,
      String content,
      String nickname,
      String email,
      String website,
      String avatar,
      String location,
      String browser,
      String os) {
    return insertAndReturnId(
        """
        insert into comments (
          target_type, target_key, parent_id, content, nickname, email, website, avatar,
          location, browser, os
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        targetType,
        targetKey,
        parentId,
        content,
        nickname,
        email,
        website,
        avatar,
        location,
        browser,
        os);
  }

  public void toggleStatus(long id) {
    jdbcTemplate.update(
        "update comments set status = case when status = 1 then 0 else 1 end where id = ?", id);
  }

  public void delete(long id) {
    jdbcTemplate.update(
        "update comments set is_deleted = true, deleted_at = current_timestamp where id = ?", id);
  }

  public Map<String, Object> find(long id) {
    return jdbcTemplate.queryForList("select * from comments where id = ?", id).getFirst();
  }

  @Transactional
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
