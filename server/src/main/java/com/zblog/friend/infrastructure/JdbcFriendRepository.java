package com.zblog.friend.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.friend.application.port.FriendRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcFriendRepository implements FriendRepository {

  private final JdbcTemplate jdbcTemplate;

  public JdbcFriendRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public PageResponse<Map<String, Object>> listTypes(int page, int pageSize) {
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select ft.id, ft.name, ft.sort_order as sort, ft.is_visible,
              count(f.id) as count
            from friend_types ft
            left join friends f on f.type_id = ft.id
            group by ft.id, ft.name, ft.sort_order, ft.is_visible
            order by ft.sort_order, ft.id
            """);
    return new PageResponse<>(list, list.size(), page, pageSize);
  }

  public long createType(String name, int sort, boolean visible) {
    return insertAndReturnId(
        "insert into friend_types (name, sort_order, is_visible) values (?, ?, ?)", name, sort, visible);
  }

  public Map<String, Object> getType(long id) {
    return jdbcTemplate
        .queryForList(
            """
            select ft.id, ft.name, ft.sort_order as sort, ft.is_visible,
              count(f.id) as count
            from friend_types ft
            left join friends f on f.type_id = ft.id
            where ft.id = ?
            group by ft.id, ft.name, ft.sort_order, ft.is_visible
            """,
            id)
        .getFirst();
  }

  public void updateType(long id, String name, int sort, boolean visible) {
    jdbcTemplate.update(
        "update friend_types set name = ?, sort_order = ?, is_visible = ? where id = ?",
        name,
        sort,
        visible,
        id);
  }

  public void deleteType(long id) {
    jdbcTemplate.update("delete from friend_types where id = ?", id);
  }

  public PageResponse<Map<String, Object>> listAdmin(int page, int pageSize) {
    List<Map<String, Object>> list =
        jdbcTemplate.queryForList(
            """
            select f.id, f.name, f.url, f.description, f.avatar, f.screenshot,
              f.sort_order as sort, f.type_id, ft.name as type_name,
              f.is_invalid, f.is_pending, f.rss_url, f.accessible
            from friends f
            left join friend_types ft on ft.id = f.type_id
            order by f.is_pending desc, f.sort_order, f.id
            """);
    return new PageResponse<>(list, list.size(), page, pageSize);
  }

  public Map<String, Object> getFriend(long id) {
    return jdbcTemplate
        .queryForList(
            """
            select f.id, f.name, f.url, f.description, f.avatar, f.screenshot,
              f.sort_order as sort, f.type_id, ft.name as type_name,
              f.is_invalid, f.is_pending, f.rss_url, f.accessible
            from friends f
            left join friend_types ft on ft.id = f.type_id
            where f.id = ?
            """,
            id)
        .getFirst();
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
    return insertAndReturnId(
        """
        insert into friends (
          name, url, description, avatar, screenshot, sort_order, type_id,
          is_invalid, is_pending, rss_url, accessible
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        name,
        url,
        description,
        avatar,
        screenshot,
        sort,
        typeId,
        invalid,
        pending,
        rssUrl,
        accessible);
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
    jdbcTemplate.update(
        """
        update friends set name = ?, url = ?, description = ?, avatar = ?, screenshot = ?,
          sort_order = ?, type_id = ?, is_invalid = ?, is_pending = ?, rss_url = ?,
          accessible = ?, updated_at = current_timestamp
        where id = ?
        """,
        name,
        url,
        description,
        avatar,
        screenshot,
        sort,
        typeId,
        invalid,
        pending,
        rssUrl,
        accessible,
        id);
  }

  public void deleteFriend(long id) {
    jdbcTemplate.update("delete from friends where id = ?", id);
  }

  public Map<String, Object> groupedPublic() {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            """
            select f.id, f.name, f.url, f.description, f.avatar, f.screenshot,
              f.sort_order as sort, f.type_id, ft.name as type_name, ft.sort_order as type_sort,
              f.is_invalid
            from friends f
            left join friend_types ft on ft.id = f.type_id
            where f.is_pending = false and (ft.is_visible = true or ft.id is null)
            order by ft.sort_order, f.sort_order, f.id
            """);
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
                next.put("type_name", row.get("type_name") == null ? "榛樿鍒嗙粍" : row.get("type_name"));
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
}
