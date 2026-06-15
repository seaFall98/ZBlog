package com.zblog.comment.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.PageResponse;
import com.zblog.comment.application.port.CommentRepository;
import com.zblog.common.exception.BusinessException;
import com.zblog.common.util.AdminDateRange;
import com.zblog.event.application.EventOutboxService;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.identity.domain.UserAccount;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CommentService {

  private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

  private final CommentRepository commentRepository;
  private final UserRepository userRepository;
  private final EventOutboxService eventOutboxService;
  private final ObjectMapper objectMapper;

  public CommentService(
      CommentRepository commentRepository,
      UserRepository userRepository,
      EventOutboxService eventOutboxService,
      ObjectMapper objectMapper) {
    this.commentRepository = commentRepository;
    this.userRepository = userRepository;
    this.eventOutboxService = eventOutboxService;
    this.objectMapper = objectMapper;
  }

  public PageResponse<Map<String, Object>> listPublic(
      String targetType, String targetKey, int page, int pageSize) {
    return listPublic(targetType, targetKey, page, pageSize, 10);
  }

  public PageResponse<Map<String, Object>> listPublic(
      String targetType, String targetKey, int page, int pageSize, int replyPageSize) {
    int safePage = Math.max(1, page);
    int safePageSize = Math.max(1, Math.min(pageSize, 100));
    int safeReplyPageSize = Math.max(1, Math.min(replyPageSize, 50));
    int offset = (safePage - 1) * safePageSize;
    List<Map<String, Object>> roots =
        commentRepository.listRootRows(targetType, targetKey, safePageSize, offset).stream()
            .map(this::publicView)
            .toList();
    List<Long> rootIds =
        roots.stream().map(comment -> ((Number) comment.get("id")).longValue()).toList();
    Map<Long, Long> replyCounts = commentRepository.countRepliesForRoots(rootIds);
    Map<Long, List<Map<String, Object>>> repliesByRoot = new LinkedHashMap<>();
    for (Map<String, Object> row : commentRepository.listInitialReplyRows(rootIds, safeReplyPageSize)) {
      long rootId = number(row.get("reply_root_id"));
      repliesByRoot.computeIfAbsent(rootId, ignored -> new ArrayList<>()).add(publicView(row));
    }
    for (Map<String, Object> root : roots) {
      long rootId = ((Number) root.get("id")).longValue();
      long replyTotal = replyCounts.getOrDefault(rootId, 0L);
      root.put("replies", repliesByRoot.getOrDefault(rootId, List.of()));
      root.put("reply_total", replyTotal);
      root.put("reply_page", 1);
      root.put("reply_page_size", safeReplyPageSize);
      root.put("reply_total_pages", totalPages(replyTotal, safeReplyPageSize));
    }
    roots.forEach(comment -> comment.putIfAbsent("replies", List.of()));
    return new PageResponse<>(roots, commentRepository.countRootRows(targetType, targetKey), safePage, safePageSize);
  }

  public PageResponse<Map<String, Object>> listReplies(long rootId, int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safePageSize = Math.max(1, Math.min(pageSize, 50));
    int offset = (safePage - 1) * safePageSize;
    List<Map<String, Object>> replies =
        commentRepository.listReplyRows(rootId, safePageSize, offset).stream().map(this::publicView).toList();
    long total = commentRepository.countReplies(rootId);
    return new PageResponse<>(replies, total, safePage, safePageSize);
  }

  public Map<String, Object> locate(String targetType, String targetKey, long commentId, int pageSize, int replyPageSize) {
    Map<String, Object> comment = commentRepository.find(commentId);
    if (!targetType.equals(text(comment, "target_type")) || !targetKey.equals(text(comment, "target_key"))) {
      throw new BusinessException(404, "Comment not found", HttpStatus.NOT_FOUND);
    }
    long rootId = rootId(comment);
    Map<String, Object> root = rootId == commentId ? comment : commentRepository.find(rootId);
    if (!targetType.equals(text(root, "target_type")) || !targetKey.equals(text(root, "target_key"))) {
      throw new BusinessException(404, "Comment not found", HttpStatus.NOT_FOUND);
    }
    int safePageSize = Math.max(1, Math.min(pageSize, 100));
    int safeReplyPageSize = Math.max(1, Math.min(replyPageSize, 50));
    long rootsBefore = commentRepository.countRootsBefore(targetType, targetKey, root.get("created_at"), rootId);
    long repliesBefore = rootId == commentId ? 0 : commentRepository.countRepliesBefore(rootId, comment.get("created_at"), commentId);
    Map<String, Object> location = new LinkedHashMap<>();
    location.put("target_type", targetType);
    location.put("target_key", targetKey);
    location.put("comment_id", commentId);
    location.put("root_id", rootId);
    location.put("root_page", (rootsBefore / safePageSize) + 1);
    location.put("reply_page", rootId == commentId ? 1 : (repliesBefore / safeReplyPageSize) + 1);
    location.put("page_size", safePageSize);
    location.put("reply_page_size", safeReplyPageSize);
    location.put("is_root", rootId == commentId);
    return location;
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
    String keywordWithWildcards = keyword != null && !keyword.isBlank() ? "%" + keyword + "%" : null;
    PageResponse<Map<String, Object>> rows =
        commentRepository.listAdminRows(
            page,
            pageSize,
            keywordWithWildcards,
            status,
            deleted,
            sub,
            dateRange.startInclusive(),
            dateRange.endExclusive());
    return new PageResponse<>(
        rows.list().stream().map(this::adminView).toList(), rows.total(), rows.page(), rows.pageSize());
  }

  public Map<String, Object> create(Map<String, Object> request, String email) {
    UserAccount user = activeUser(email);
    String targetType = requiredText(request, "target_type");
    String targetKey = requiredText(request, "target_key");
    String content = requiredText(request, "content");
    if (content.length() > 2000) {
      throw new BusinessException(400, "Comment content is too long", HttpStatus.BAD_REQUEST);
    }
    Long parentId = nullableNumber(request, "parent_id");
    Long rootId = null;
    Long recipientUserId = null;
    if (parentId != null) {
      Map<String, Object> parent = commentRepository.find(parentId);
      if (!targetType.equals(text(parent, "target_type")) || !targetKey.equals(text(parent, "target_key"))) {
        throw new BusinessException(400, "Invalid parent comment", HttpStatus.BAD_REQUEST);
      }
      rootId = rootIdFromParent(parent, parentId);
      Object owner = parent.get("user_id");
      if (owner instanceof Number number && number.longValue() > 0 && number.longValue() != user.id()) {
        recipientUserId = number.longValue();
      }
    }
    long id =
        commentRepository.create(
            targetType,
            targetKey,
            parentId,
            content,
            user.nickname(),
            user.email(),
            user.website(),
            user.avatar(),
            user.id(),
            rootId);
    if (recipientUserId != null) {
      Map<String, Object> eventPayload = new LinkedHashMap<>();
      eventPayload.put("recipient_user_id", recipientUserId);
      eventPayload.put("actor_user_id", user.id());
      eventPayload.put("actor_nickname", user.nickname());
      eventPayload.put("target_type", targetType);
      eventPayload.put("target_key", targetKey);
      eventPayload.put("comment_id", id);
      eventPayload.put("parent_id", parentId);
      eventPayload.put("content", content);
      eventOutboxService.createCommentReplyEvent(
          eventPayload);
    }
    return publicView(commentRepository.find(id));
  }

  public Map<String, Object> createAdmin(Map<String, Object> request, String email) {
    UserAccount user = activeUser(email);
    String targetType = requiredText(request, "target_type");
    String targetKey = requiredText(request, "target_key");
    String content = requiredText(request, "content");
    if (content.length() > 2000) {
      throw new BusinessException(400, "Comment content is too long", HttpStatus.BAD_REQUEST);
    }
    Long parentId = nullableNumber(request, "parent_id");
    Long rootId = null;
    Long recipientUserId = null;
    if (parentId != null) {
      Map<String, Object> parent = commentRepository.find(parentId);
      if (!targetType.equals(text(parent, "target_type")) || !targetKey.equals(text(parent, "target_key"))) {
        throw new BusinessException(400, "Invalid parent comment", HttpStatus.BAD_REQUEST);
      }
      rootId = rootIdFromParent(parent, parentId);
      Object owner = parent.get("user_id");
      if (owner instanceof Number number && number.longValue() > 0 && number.longValue() != user.id()) {
        recipientUserId = number.longValue();
      }
    }
    long id =
        commentRepository.create(
            targetType,
            targetKey,
            parentId,
            content,
            user.nickname(),
            user.email(),
            user.website(),
            user.avatar(),
            user.id(),
            rootId);
    if (recipientUserId != null) {
      Map<String, Object> eventPayload = new LinkedHashMap<>();
      eventPayload.put("recipient_user_id", recipientUserId);
      eventPayload.put("actor_user_id", user.id());
      eventPayload.put("actor_nickname", user.nickname());
      eventPayload.put("target_type", targetType);
      eventPayload.put("target_key", targetKey);
      eventPayload.put("comment_id", id);
      eventPayload.put("parent_id", parentId);
      eventPayload.put("content", content);
      eventOutboxService.createCommentReplyEvent(eventPayload);
    }
    return adminView(commentRepository.find(id));
  }

  public Map<String, Object> toggleStatus(long id) {
    commentRepository.toggleStatus(id);
    return adminView(commentRepository.find(id));
  }

  public void delete(long id) {
    commentRepository.delete(id);
  }

  public void deletePublic(long id, String email) {
    UserAccount user = activeUser(email);
    Map<String, Object> comment = commentRepository.find(id);
    Object owner = comment.get("user_id");
    boolean isOwner = owner instanceof Number number && number.longValue() == user.id();
    boolean isAdmin = "admin".equalsIgnoreCase(user.role()) || "super_admin".equalsIgnoreCase(user.role());
    if (!isOwner && !isAdmin) {
      throw new BusinessException(403, "Forbidden", HttpStatus.FORBIDDEN);
    }
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

  private Map<String, Object> publicView(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    boolean deleted = Boolean.TRUE.equals(row.get("is_deleted"));
    boolean userDeleted = row.get("user_deleted_at") != null;
    view.put("id", row.get("id"));
    view.put("content", deleted ? "评论已删除" : row.get("content"));
    view.put("is_deleted", deleted);
    view.put("parent_id", row.get("parent_id"));
    view.put("root_id", rootId(row));
    view.put("created_at", formatShanghai(row.get("created_at")));
    view.put("can_delete", false);
    String nickname = deleted || userDeleted ? "已注销用户" : publicNickname(row);
    String avatar = deleted || userDeleted ? "" : publicAvatar(row);
    String website = deleted || userDeleted ? "" : publicWebsite(row);
    String badge = deleted || userDeleted ? "" : text(row, "user_badge");
    String role = StringUtils.hasText(text(row, "user_role")) ? text(row, "user_role") : "guest";
    Map<String, Object> user = new LinkedHashMap<>();
    user.put("role", role);
    user.put("id", row.get("user_id") == null ? 0 : row.get("user_id"));
    user.put("email_hash", Integer.toHexString(text(row, "email").hashCode()));
    user.put("nickname", nickname);
    user.put("avatar", avatar);
    user.put("badge", badge);
    user.put("website", website);
    view.put("user", user);
    view.put("reply_user", replyUserView(row));
    view.put("like_count", 0);
    view.put("liked_by_me", false);
    view.put("reply_total", 0);
    view.put("reply_page", 1);
    view.put("reply_page_size", 10);
    view.put("reply_total_pages", 0);
    view.put("replies", List.of());
    return view;
  }

  private Map<String, Object> adminView(Map<String, Object> row) {
    Map<String, Object> view = new LinkedHashMap<>();
    view.put("id", row.get("id"));
    view.put("content", row.get("content"));
    view.put("status", row.get("status"));
    view.put("parent_id", row.get("parent_id"));
    view.put("created_at", formatShanghai(row.get("created_at")));
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

  private Map<String, Object> replyUserView(Map<String, Object> row) {
    Object replyUserId = row.get("reply_user_id");
    if (!(replyUserId instanceof Number number) || number.longValue() <= 0) {
      String fallback = text(row, "reply_user_fallback_nickname");
      if (!StringUtils.hasText(fallback)) {
        return null;
      }
      return Map.of(
          "id", 0,
          "nickname", fallback,
          "avatar", "",
          "badge", "",
          "website", "",
          "role", "guest");
    }
    boolean deleted = row.get("reply_user_deleted_at") != null;
    return Map.of(
        "id", deleted ? 0 : number.longValue(),
        "nickname", deleted ? "已注销用户" : textOrDefault(row, "reply_user_nickname", "Guest"),
        "avatar", deleted ? "" : text(row, "reply_user_avatar"),
        "badge", deleted ? "" : text(row, "reply_user_badge"),
        "website", deleted ? "" : text(row, "reply_user_website"),
        "role", deleted ? "guest" : textOrDefault(row, "reply_user_role", "guest"));
  }

  private long rootId(Map<String, Object> row) {
    Object id = row.get("id");
    Object root = row.get("root_id");
    if (root instanceof Number number && number.longValue() > 0) {
      return number.longValue();
    }
    Object parent = row.get("parent_id");
    if (parent instanceof Number number && number.longValue() > 0) {
      return number.longValue();
    }
    return id instanceof Number number ? number.longValue() : 0;
  }

  private long rootIdFromParent(Map<String, Object> parent, long parentId) {
    Object root = parent.get("root_id");
    if (root instanceof Number number && number.longValue() > 0) {
      return number.longValue();
    }
    Object parentParent = parent.get("parent_id");
    if (parentParent instanceof Number number && number.longValue() > 0) {
      return number.longValue();
    }
    return parentId;
  }

  private long number(Object value) {
    return value instanceof Number number ? number.longValue() : 0;
  }

  private long totalPages(long total, int pageSize) {
    if (total <= 0) {
      return 0;
    }
    return (total + pageSize - 1) / pageSize;
  }

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString();
  }

  private String formatShanghai(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof LocalDateTime dateTime) {
      return dateTime.atZone(SHANGHAI_ZONE).toOffsetDateTime().toString();
    }
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toInstant().atZone(SHANGHAI_ZONE).toOffsetDateTime().toString();
    }
    if (value instanceof Instant instant) {
      return instant.atZone(SHANGHAI_ZONE).toOffsetDateTime().toString();
    }
    return value.toString();
  }

  private String requiredText(Map<String, Object> request, String key) {
    String value = text(request, key).trim();
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(400, key + " is required", HttpStatus.BAD_REQUEST);
    }
    return value;
  }

  private UserAccount activeUser(String email) {
    UserAccount user = userRepository.findByEmail(email);
    if (!user.enabled() || user.deletedAt() != null) {
      throw new BusinessException(401, "User is disabled", HttpStatus.UNAUTHORIZED);
    }
    return user;
  }

  private String publicNickname(Map<String, Object> row) {
    return textOrDefault(row, "user_nickname", textOrDefault(row, "nickname", "Guest"));
  }

  private String publicAvatar(Map<String, Object> row) {
    String userAvatar = text(row, "user_avatar");
    return StringUtils.hasText(userAvatar) ? userAvatar : text(row, "avatar");
  }

  private String publicWebsite(Map<String, Object> row) {
    String userWebsite = text(row, "user_website");
    return StringUtils.hasText(userWebsite) ? userWebsite : text(row, "website");
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
