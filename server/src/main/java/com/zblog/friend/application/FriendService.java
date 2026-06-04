package com.zblog.friend.application;

import com.zblog.common.api.PageResponse;
import com.zblog.friend.application.port.FriendRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FriendService {

  private final FriendRepository friendRepository;

  public FriendService(FriendRepository friendRepository) {
    this.friendRepository = friendRepository;
  }

  public PageResponse<Map<String, Object>> listTypes(int page, int pageSize) {
    return friendRepository.listTypes(page, pageSize);
  }

  public Map<String, Object> createType(Map<String, Object> request) {
    long id =
        friendRepository.createType(
            text(request, "name"), number(request, "sort", 0), bool(request, "is_visible", true));
    return getType(id);
  }

  public Map<String, Object> getType(long id) {
    return friendRepository.getType(id);
  }

  public Map<String, Object> updateType(long id, Map<String, Object> request) {
    friendRepository.updateType(
        id, text(request, "name"), number(request, "sort", 0), bool(request, "is_visible", true));
    return getType(id);
  }

  public void deleteType(long id) {
    friendRepository.deleteType(id);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    return friendRepository.listAdmin(page, pageSize);
  }

  public Map<String, Object> getFriend(long id) {
    return friendRepository.getFriend(id);
  }

  public Map<String, Object> createFriend(Map<String, Object> request) {
    long id = insertFriend(request, false);
    return getFriend(id);
  }

  public Map<String, Object> applyFriend(Map<String, Object> request) {
    long id = insertFriend(request, true);
    return getFriend(id);
  }

  public Map<String, Object> updateFriend(long id, Map<String, Object> request) {
    friendRepository.updateFriend(
        id,
        text(request, "name"),
        text(request, "url"),
        text(request, "description"),
        text(request, "avatar"),
        text(request, "screenshot"),
        number(request, "sort", 0),
        nullableNumber(request, "type_id"),
        bool(request, "is_invalid", false),
        bool(request, "is_pending", false),
        text(request, "rss_url"),
        number(request, "accessible", 0));
    return getFriend(id);
  }

  public void deleteFriend(long id) {
    friendRepository.deleteFriend(id);
  }

  public Map<String, Object> groupedPublic() {
    return friendRepository.groupedPublic();
  }

  private long insertFriend(Map<String, Object> request, boolean pending) {
    return friendRepository.createFriend(
        text(request, "name"),
        text(request, "url"),
        text(request, "description"),
        text(request, "avatar"),
        text(request, "screenshot"),
        number(request, "sort", 0),
        nullableNumber(request, "type_id"),
        bool(request, "is_invalid", false),
        pending || bool(request, "is_pending", false),
        text(request, "rss_url"),
        number(request, "accessible", 0));
  }

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString();
  }

  private int number(Map<String, Object> request, String key, int fallback) {
    Object value = request.get(key);
    return value instanceof Number number ? number.intValue() : fallback;
  }

  private Long nullableNumber(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value instanceof Number number ? number.longValue() : null;
  }

  private boolean bool(Map<String, Object> request, String key, boolean fallback) {
    Object value = request.get(key);
    return value instanceof Boolean bool ? bool : fallback;
  }
}
