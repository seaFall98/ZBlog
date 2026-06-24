package com.zblog.comment.infrastructure.mybatis;

import com.zblog.comment.application.port.CommentRepository;
import com.zblog.common.api.PageResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisCommentRepository implements CommentRepository {

  private final CommentMapper commentMapper;

  public MyBatisCommentRepository(CommentMapper commentMapper) {
    this.commentMapper = commentMapper;
  }

  public long countRootRows(String targetType, String targetKey) {
    return commentMapper.countRootRows(targetType, targetKey);
  }

  public long countAllPublicRows(String targetType, String targetKey) {
    return commentMapper.countAllPublicRows(targetType, targetKey);
  }

  public long countPublicRowsByUser(long userId) {
    return commentMapper.countPublicRowsByUser(userId);
  }

  public List<Map<String, Object>> listRootRows(
      String targetType, String targetKey, int limit, int offset, String sort) {
    return commentMapper.listRootRows(targetType, targetKey, limit, offset, sort);
  }

  public List<Map<String, Object>> listInitialReplyRows(List<Long> rootIds, int limitPerRoot) {
    if (rootIds.isEmpty()) {
      return List.of();
    }
    return commentMapper.listInitialReplyRows(rootIds, limitPerRoot);
  }

  public long countReplies(long rootId) {
    return commentMapper.countReplies(rootId);
  }

  public Map<Long, Long> countRepliesForRoots(List<Long> rootIds) {
    Map<Long, Long> counts = new LinkedHashMap<>();
    for (Long rootId : rootIds) {
      counts.put(rootId, 0L);
    }
    if (rootIds.isEmpty()) {
      return counts;
    }
    for (Map<String, Object> row : commentMapper.countRepliesForRoots(rootIds)) {
      Object root = row.get("root_id");
      Object total = row.get("total");
      if (root instanceof Number rootNumber && total instanceof Number totalNumber) {
        counts.put(rootNumber.longValue(), totalNumber.longValue());
      }
    }
    return counts;
  }

  public List<Map<String, Object>> listReplyRows(long rootId, int limit, int offset) {
    return commentMapper.listReplyRows(rootId, limit, offset);
  }

  public long countRootsBefore(String targetType, String targetKey, Object createdAt, long id) {
    return commentMapper.countRootsBefore(targetType, targetKey, createdAt, id);
  }

  public long countRepliesBefore(long rootId, Object createdAt, long id) {
    return commentMapper.countRepliesBefore(rootId, createdAt, id);
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
    String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.toLowerCase() + "%";
    long total = commentMapper.countAdminRows(like, status, deleted, sub, start, end);
    return new PageResponse<>(
        commentMapper.listAdminRows(like, status, deleted, sub, start, end, pageSize, offset),
        total,
        page,
        pageSize);
  }

  public long create(
      String targetType,
      String targetKey,
      Long parentId,
      String content,
      String nickname,
      String email,
      String website,
      String avatar,
      long userId,
      Long rootId) {
    Map<String, Object> params = baseParams(targetType, targetKey, parentId, content, nickname, email, website, avatar);
    params.put("userId", userId);
    params.put("rootId", rootId);
    commentMapper.insertComment(params);
    return generatedId(params);
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
    Map<String, Object> params = baseParams(targetType, targetKey, parentId, content, nickname, email, website, avatar);
    params.put("location", location);
    params.put("browser", browser);
    params.put("os", os);
    commentMapper.insertImportedComment(params);
    return generatedId(params);
  }

  public void toggleStatus(long id) {
    commentMapper.toggleStatus(id);
  }

  public Map<Long, Boolean> likedByUser(long userId, List<Long> commentIds) {
    Map<Long, Boolean> result = new LinkedHashMap<>();
    for (Long commentId : commentIds) {
      result.put(commentId, false);
    }
    if (commentIds.isEmpty()) {
      return result;
    }
    for (Map<String, Object> row : commentMapper.likedByUser(userId, commentIds)) {
      Object commentId = row.get("comment_id");
      if (commentId instanceof Number number) {
        result.put(number.longValue(), true);
      }
    }
    return result;
  }

  public boolean addLike(long commentId, long userId) {
    return commentMapper.insertLike(commentId, userId) > 0;
  }

  public boolean removeLike(long commentId, long userId) {
    return commentMapper.deleteLike(commentId, userId) > 0;
  }

  public void incrementLikeCount(long commentId) {
    commentMapper.incrementLikeCount(commentId);
  }

  public void decrementLikeCount(long commentId) {
    commentMapper.decrementLikeCount(commentId);
  }

  public void pin(long id, long operatorUserId) {
    commentMapper.pin(id, operatorUserId);
  }

  public void unpin(long id) {
    commentMapper.unpin(id);
  }

  public int delete(long id) {
    return commentMapper.delete(id);
  }

  public Map<String, Object> find(long id) {
    return commentMapper.rowsById(id).getFirst();
  }

  private Map<String, Object> baseParams(
      String targetType,
      String targetKey,
      Long parentId,
      String content,
      String nickname,
      String email,
      String website,
      String avatar) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("targetType", targetType);
    params.put("targetKey", targetKey);
    params.put("parentId", parentId);
    params.put("content", content);
    params.put("nickname", nickname);
    params.put("email", email);
    params.put("website", website);
    params.put("avatar", avatar);
    return params;
  }

  private long generatedId(Map<String, Object> params) {
    return ((Number) params.get("id")).longValue();
  }
}
