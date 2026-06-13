package com.zblog.friend.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.friend.application.port.FriendRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisFriendRepository implements FriendRepository {

  private final FriendMapper friendMapper;

  public MyBatisFriendRepository(FriendMapper friendMapper) {
    this.friendMapper = friendMapper;
  }

  public PageResponse<Map<String, Object>> listTypes(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    var list = friendMapper.listTypes(offset, normalizedPageSize);
    long total = friendMapper.countTypes();
    return new PageResponse<>(list, total, normalizedPage, normalizedPageSize);
  }

  public long createType(String name, int sort, boolean visible) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("name", name);
    params.put("sort", sort);
    params.put("visible", visible);
    friendMapper.insertType(params);
    return generatedId(params);
  }

  public Map<String, Object> getType(long id) {
    List<Map<String, Object>> rows = friendMapper.typeById(id);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Friend type not found", HttpStatus.NOT_FOUND);
    }
    return rows.getFirst();
  }

  public void updateType(long id, String name, int sort, boolean visible) {
    friendMapper.updateType(id, name, sort, visible);
  }

  public void deleteType(long id) {
    friendMapper.deleteType(id);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    int normalizedPage = Math.max(page, 1);
    int normalizedPageSize = Math.max(pageSize, 1);
    int offset = (normalizedPage - 1) * normalizedPageSize;
    var list = friendMapper.listAdmin(offset, normalizedPageSize);
    long total = friendMapper.countAdmin();
    return new PageResponse<>(list, total, normalizedPage, normalizedPageSize);
  }

  public Map<String, Object> getFriend(long id) {
    List<Map<String, Object>> rows = friendMapper.friendById(id);
    if (rows.isEmpty()) {
      throw new BusinessException(404, "Friend not found", HttpStatus.NOT_FOUND);
    }
    return rows.getFirst();
  }

  public long createFriend(
      String name,
      String url,
      String description,
      String avatar,
      String screenshot,
      int sort,
      Long typeId,
      boolean invalid,
      boolean pending,
      String rssUrl,
      int accessible) {
    Map<String, Object> params =
        friendParams(0, name, url, description, avatar, screenshot, sort, typeId, invalid, pending, rssUrl, accessible);
    friendMapper.insertFriend(params);
    return generatedId(params);
  }

  public void updateFriend(
      long id,
      String name,
      String url,
      String description,
      String avatar,
      String screenshot,
      int sort,
      Long typeId,
      boolean invalid,
      boolean pending,
      String rssUrl,
      int accessible) {
    friendMapper.updateFriend(
        friendParams(id, name, url, description, avatar, screenshot, sort, typeId, invalid, pending, rssUrl, accessible));
  }

  public void deleteFriend(long id) {
    friendMapper.deleteFriend(id);
  }

  public Map<String, Object> groupedPublic() {
    List<Map<String, Object>> rows = friendMapper.groupedPublicRows();
    Map<Long, Map<String, Object>> groups = new LinkedHashMap<>();
    for (Map<String, Object> row : rows) {
      Long typeId = row.get("type_id") instanceof Number n ? n.longValue() : null;
      long key = typeId == null ? 0 : typeId;
      Map<String, Object> group =
          groups.computeIfAbsent(
              key,
              ignored -> {
                Map<String, Object> next = new LinkedHashMap<>();
                next.put("type_id", typeId);
                next.put("type_name", row.get("type_name") == null ? "未分类" : row.get("type_name"));
                next.put("type_sort", row.get("type_sort") == null ? 0 : row.get("type_sort"));
                next.put("friends", new java.util.ArrayList<Map<String, Object>>());
                return next;
              });
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> friends = (List<Map<String, Object>>) group.get("friends");
      friends.add(row);
    }
    return Map.of(
        "groups", List.copyOf(groups.values()),
        "total_groups", groups.size(),
        "total_friends", rows.size());
  }

  private Map<String, Object> friendParams(
      long id,
      String name,
      String url,
      String description,
      String avatar,
      String screenshot,
      int sort,
      Long typeId,
      boolean invalid,
      boolean pending,
      String rssUrl,
      int accessible) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("id", id);
    params.put("name", name);
    params.put("url", url);
    params.put("description", description);
    params.put("avatar", avatar);
    params.put("screenshot", screenshot);
    params.put("sort", sort);
    params.put("typeId", typeId);
    params.put("invalid", invalid);
    params.put("pending", pending);
    params.put("rssUrl", rssUrl);
    params.put("accessible", accessible);
    return params;
  }

  private long generatedId(Map<String, Object> params) {
    return ((Number) params.get("id")).longValue();
  }
}
