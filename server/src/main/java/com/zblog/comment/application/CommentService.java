package com.zblog.comment.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.comment.application.port.CommentRepository;
import com.zblog.common.util.AdminDateRange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CommentService {

  private final CommentRepository commentRepository;
  private final ObjectMapper objectMapper;

  public CommentService(CommentRepository commentRepository, ObjectMapper objectMapper) {
    this.commentRepository = commentRepository;
    this.objectMapper = objectMapper;
  }

  public PageResponse<Map<String, Object>> listPublic(
      String targetType, String targetKey, int page, int pageSize) {
    List<Map<String, Object>> flat =
        commentRepository.listPublicRows(targetType, targetKey).stream().map(this::publicView).toList();
    return new PageResponse<>(nest(flat), flat.size(), page, pageSize);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    return listAdmin(page, pageSize, null, null, null, null, null, null);
  }

  public PageResponse<Map<String, Object>> listAdmin(
      int page,
      int pageSize,
      String keyword,
      Integer status,
      Boolean deleted,
      Boolean sub,
      String startTime,
      String endTime) {
    AdminDateRange dateRange = AdminDateRange.parse(startTime, endTime);
    PageResponse<Map<String, Object>> rows =
        commentRepository.listAdminRows(
            page,
            pageSize,
            keyword,
            status,
            deleted,
            sub,
            dateRange.startInclusive(),
            dateRange.endExclusive());
    return new PageResponse<>(
        rows.list().stream().map(this::adminView).toList(), rows.total(), rows.page(), rows.pageSize());
  }

  public Map<String, Object> create(Map<String, Object> request) {
    long id =
        commentRepository.create(
            text(request, "target_type"),
            text(request, "target_key"),
            nullableNumber(request, "parent_id"),
            text(request, "content"),
            textOrDefault(request, "nickname", "Guest"),
            text(request, "email"),
            text(request, "website"),
            "");
    return publicView(commentRepository.find(id));
  }

  public Map<String, Object> toggleStatus(long id) {
    commentRepository.toggleStatus(id);
    return adminView(commentRepository.find(id));
  }

  public void delete(long id) {
    commentRepository.delete(id);
  }

  public Map<String, Object> importComments(String sourceType, MultipartFile file) {
    List<Map<String, Object>> errors = new ArrayList<>();
    int success = 0;
    List<Map<String, Object>> comments;
    try {
      String raw = new String(file.getBytes(), StandardCharsets.UTF_8);
      comments = parseCommentItems(raw);
    } catch (IOException | RuntimeException exception) {
      return Map.of(
          "total",
          0,
          "success",
          0,
          "failed",
          1,
          "user_created",
          0,
          "errors",
          List.of(Map.of("index", 0, "content", "", "error", exception.getMessage())));
    }

    for (int i = 0; i < comments.size(); i++) {
      Map<String, Object> comment = comments.get(i);
      String content = firstText(comment, "content", "body", "text");
      try {
        if (content.isBlank()) {
          throw new IllegalArgumentException("Comment content is required");
        }
        String targetKey = firstText(comment, "target_key", "page_key", "path", "url");
        if (targetKey.isBlank()) {
          throw new IllegalArgumentException("target_key is required");
        }
        commentRepository.importComment(
            textOrDefault(comment, "target_type", "article"),
            targetKey,
            nullableNumber(comment, "parent_id"),
            content,
            firstTextOrDefault(comment, "nickname", "Guest", "nick", "name"),
            text(comment, "email"),
            firstText(comment, "website", "link"),
            text(comment, "avatar"),
            text(comment, "location"),
            text(comment, "browser"),
            text(comment, "os"));
        success++;
      } catch (RuntimeException exception) {
        errors.add(Map.of("index", i, "content", content, "error", exception.getMessage()));
      }
    }
    return Map.of(
        "total",
        comments.size(),
        "success",
        success,
        "failed",
        errors.size(),
        "user_created",
        0,
        "errors",
        errors);
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

  private String firstText(Map<String, Object> request, String key, String... aliases) {
    String value = text(request, key);
    if (!value.isBlank()) {
      return value;
    }
    for (String alias : aliases) {
      value = text(request, alias);
      if (!value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String firstTextOrDefault(
      Map<String, Object> request, String key, String fallback, String... aliases) {
    String value = firstText(request, key, aliases);
    return value.isBlank() ? fallback : value;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> parseCommentItems(String raw) throws IOException {
    Object parsed = objectMapper.readValue(raw, new TypeReference<Object>() {});
    Object list = parsed;
    if (parsed instanceof Map<?, ?> map) {
      if (map.containsKey("comments")) {
        list = map.get("comments");
      } else if (map.containsKey("data")) {
        list = map.get("data");
      } else {
        list = List.of();
      }
    }
    if (!(list instanceof List<?> items)) {
      throw new IllegalArgumentException("Comment import file must contain an array");
    }
    List<Map<String, Object>> comments = new ArrayList<>();
    for (Object item : items) {
      if (item instanceof Map<?, ?> map) {
        comments.add((Map<String, Object>) map);
      }
    }
    return comments;
  }
}
