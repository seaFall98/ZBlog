package com.zblog.comment.application.port;

import com.zblog.common.api.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CommentRepository {

  List<Map<String, Object>> listPublicRows(String targetType, String targetKey);

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
      String avatar);

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

  void delete(long id);

  Map<String, Object> find(long id);
}
