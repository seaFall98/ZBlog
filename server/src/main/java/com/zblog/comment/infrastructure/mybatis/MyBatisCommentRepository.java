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

  public List<Map<String, Object>> listPublicRows(String targetType, String targetKey) {
    return commentMapper.listPublicRows(targetType, targetKey);
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
      String avatar) {
    Map<String, Object> params = baseParams(targetType, targetKey, parentId, content, nickname, email, website, avatar);
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

  public void delete(long id) {
    commentMapper.delete(id);
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
