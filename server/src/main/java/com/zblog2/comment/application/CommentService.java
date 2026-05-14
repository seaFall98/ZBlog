package com.zblog2.comment.application;

import com.zblog2.common.api.PageResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  private final JdbcTemplate jdbcTemplate;

  public CommentService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PageResponse<Map<String, Object>> listPublic(
      String targetType, String targetKey, int page, int pageSize) {
    List<Map<String, Object>> flat =
        jdbcTemplate
            .queryForList(
                """
                select * from comments
                where target_type = ? and target_key = ? and status = 1 and is_deleted = false
                order by created_at desc, id desc
                """,
                targetType,
                targetKey)
            .stream()
            .map(this::publicView)
            .toList();
    return new PageResponse<>(nest(flat), flat.size(), page, pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    List<Map<String, Object>> list =
        jdbcTemplate
            .queryForList("select * from comments order by created_at desc, id desc")
            .stream()
            .map(this::adminView)
            .toList();
    return new PageResponse<>(list, list.size(), page, pageSize);
  }

  public Map<String, Object> create(Map<String, Object> request) {
    long id =
        insertAndReturnId(
            """
            insert into comments (
              target_type, target_key, parent_id, content, nickname, email, website, avatar
            ) values (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            text(request, "target_type"),
            text(request, "target_key"),
            nullableNumber(request, "parent_id"),
            text(request, "content"),
            textOrDefault(request, "nickname", "Guest"),
            text(request, "email"),
            text(request, "website"),
            "");
    return publicView(find(id));
  }

  public Map<String, Object> toggleStatus(long id) {
    jdbcTemplate.update(
        "update comments set status = case when status = 1 then 0 else 1 end where id = ?", id);
    return adminView(find(id));
  }

  public void delete(long id) {
    jdbcTemplate.update(
        "update comments set is_deleted = true, deleted_at = current_timestamp where id = ?", id);
  }

  private List<Map<String, Object>> nest(List<Map<String, Object>> flat) {
    Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
    java.util.ArrayList<Map<String, Object>> roots = new java.util.ArrayList<>();
    for (Map<String, Object> comment : flat) {
      comment.put("replies", new java.util.ArrayList<Map<String, Object>>());
      byId.put(((Number) comment.get("id")).longValue(), comment);
    }
    for (Map<String, Object> comment : flat) {
      Object parent = comment.get("parent_id");
      if (parent instanceof Number number && byId.containsKey(number.longValue())) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> replies =
            (List<Map<String, Object>>) byId.get(number.longValue()).get("replies");
        replies.add(comment);
      } else {
        roots.add(comment);
      }
    }
    return roots;
  }

  private Map<String, Object> publicView(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", row.get("id"));
    view.put("content", row.get("content"));
    view.put("is_deleted", row.get("is_deleted"));
    view.put("parent_id", row.get("parent_id"));
    view.put("created_at", row.get("created_at").toString());
    view.put("location", row.get("location"));
    view.put("browser", row.get("browser"));
    view.put("os", row.get("os"));
    view.put(
        "user",
        Map.of(
            "role", "guest",
            "id", 0,
            "email_hash", Integer.toHexString(text(row, "email").hashCode()),
            "nickname", textOrDefault(row, "nickname", "Guest"),
            "avatar", text(row, "avatar"),
            "website", text(row, "website")));
    view.put("replies", List.of());
    return view;
  }

  private Map<String, Object> adminView(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", row.get("id"));
    view.put("content", row.get("content"));
    view.put("status", row.get("status"));
    view.put("parent_id", row.get("parent_id"));
    view.put("created_at", row.get("created_at").toString());
    view.put("deleted_at", row.get("deleted_at"));
    view.put(
        "target",
        Map.of(
            "type", row.get("target_type"),
            "key", row.get("target_key"),
            "title", row.get("target_key")));
    view.put(
        "user",
        Map.of(
            "id", 0,
            "nickname", textOrDefault(row, "nickname", "Guest"),
            "email", text(row, "email"),
            "avatar", text(row, "avatar")));
    return view;
  }

  private Map<String, Object> find(long id) {
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

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString();
  }

  private String textOrDefault(Map<String, Object> request, String key, String fallback) {
    String value = text(request, key);
    return value.isBlank() ? fallback : value;
  }

  private Long nullableNumber(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value instanceof Number number ? number.longValue() : null;
  }
}
