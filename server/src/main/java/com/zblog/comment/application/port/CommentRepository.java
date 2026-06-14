package com.zblog.comment.application.port;

import com.zblog.common.api.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CommentRepository {

  long countRootRows(String targetType, String targetKey);

  long countAllPublicRows(String targetType, String targetKey);

  List<Map<String, Object>> listRootRows(String targetType, String targetKey, int limit, int offset);

  List<Map<String, Object>> listInitialReplyRows(List<Long> rootIds, int limitPerRoot);

  long countReplies(long rootId);

  Map<Long, Long> countRepliesForRoots(List<Long> rootIds);

  List<Map<String, Object>> listReplyRows(long rootId, int limit, int offset);

  long countRootsBefore(String targetType, String targetKey, Object createdAt, long id);

  long countRepliesBefore(long rootId, Object createdAt, long id);

  PageResponse<Map<String, Object>> listAdminRows(
      int page,
      int pageSize,
      String keyword,
      Integer status,
      Boolean deleted,
      Boolean sub,
      LocalDateTime start,
      LocalDateTime end);

  long create(
      String targetType,
      String targetKey,
      Long parentId,
      String content,
      String nickname,
      String email,
      String website,
      String avatar,
      long userId,
      Long rootId);

  long importComment(
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
      String os);

  void toggleStatus(long id);

  int delete(long id);

  Map<String, Object> find(long id);
}
