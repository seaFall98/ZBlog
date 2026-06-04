package com.zblog.friend.application.port;

import com.zblog.common.api.PageResponse;
import java.util.Map;

public interface FriendRepository {

  PageResponse<Map<String, Object>> listTypes(int page, int pageSize);

  long createType(String name, int sort, boolean visible);

  Map<String, Object> getType(long id);

  void updateType(long id, String name, int sort, boolean visible);

  void deleteType(long id);

  PageResponse<Map<String, Object>> listAdmin(int page, int pageSize);

  Map<String, Object> getFriend(long id);

  long createFriend(
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
      int accessible);

  void updateFriend(
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
      int accessible);

  void deleteFriend(long id);

  Map<String, Object> groupedPublic();
}
