package db.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V20__front_v2_settings_and_menus extends BaseJavaMigration {

  private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {};

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    Map<String, String> basic = loadGroup(connection, "basic");
    Map<String, String> blog = loadGroup(connection, "blog");

    migrateV2Settings(connection, basic, blog);
    migrateMenus(connection);
  }

  private void migrateV2Settings(Connection connection, Map<String, String> basic, Map<String, String> blog)
      throws SQLException, JsonProcessingException {
    upsertIfBlank(connection, "v2_identity", "site_title", firstNonBlank(blog, basic, "title", "site_title", "site_name"));
    upsertIfBlank(connection, "v2_identity", "owner_display_name", firstNonBlank(basic, blog, "author"));
    upsertIfBlank(
        connection,
        "v2_identity",
        "email",
        firstNonBlank(basic, blog, "author_email", "contact_email", "email"));
    upsertIfBlank(
        connection,
        "v2_identity",
        "primary_image_url",
        firstNonBlank(basic, blog, "author_photo", "author_avatar", "avatar_url", "avatar"));
    upsertIfBlank(connection, "v2_identity", "favicon_url", firstNonBlank(blog, basic, "favicon", "favicon_url"));
    upsertIfBlank(connection, "v2_identity", "icp_record", firstNonBlank(basic, blog, "icp"));
    upsertIfBlank(connection, "v2_identity", "police_record", firstNonBlank(basic, blog, "police_record"));

    upsertIfBlank(connection, "v2_home", "hero_eyebrow", firstNonBlank(blog, basic, "hero_eyebrow"));
    upsertIfBlank(connection, "v2_home", "hero_title", firstNonBlank(blog, basic, "hero_title"));
    upsertIfBlank(connection, "v2_home", "hero_meta", "");
    upsertIfBlank(connection, "v2_home", "hero_cta_label", "阅读文章");
    upsertIfBlank(connection, "v2_home", "hero_cta_target", "/blog");

    upsertIfBlank(
        connection,
        "v2_about",
        "intro_text",
        firstNonBlank(blog, basic, "about_describe", "author_desc"));
    upsertIfBlank(connection, "v2_about", "status_items", objectMapper.writeValueAsString(migrateStatusItems(read(blog, "about_profile"))));
    upsertIfBlank(connection, "v2_about", "skill_items", objectMapper.writeValueAsString(migrateSkillItems(read(blog, "about_profile"))));
    upsertIfBlank(connection, "v2_about", "timeline_items", objectMapper.writeValueAsString(migrateTimelineItems(read(blog, "about_story"))));
    upsertIfBlank(connection, "v2_about", "bottom_quote", migrateBottomQuote(read(blog, "about_motto_main")));

    upsertIfBlank(connection, "v2_guestbook", "intro_text", firstNonBlank(blog, basic, "message_content"));

    upsertIfBlank(
        connection,
        "v2_footer",
        "description",
        firstNonBlank(blog, basic, "footer_description", "description"));
    upsertIfBlank(connection, "v2_footer", "copyright_text", firstNonBlank(blog, basic, "footer_copyright"));
    upsertIfBlank(connection, "v2_footer", "slogan", firstNonBlank(blog, basic, "footer_slogan", "slogan"));
    upsertIfBlank(connection, "v2_footer", "social_links", objectMapper.writeValueAsString(migrateSocialLinks(read(blog, "footer_social"))));
  }

  private void migrateMenus(Connection connection) throws SQLException {
    execute(connection, "delete from menus where type in ('aggregate', 'navigation', 'header_navigation')");
    execute(connection, "update menus set type = 'footer_navigation' where type = 'footer'");
    execute(connection, "update menus set url = '/gallery' where type = 'footer_navigation' and url = '/album'");
    execute(connection, "update menus set url = '/moments' where type = 'footer_navigation' and url = '/moment'");
    execute(connection, "update menus set url = '/guestbook' where type = 'footer_navigation' and url = '/message'");
    execute(connection, "update menus set url = '/stats' where type = 'footer_navigation' and url = '/statistics'");
    execute(connection, "update menus set url = '/links' where type = 'footer_navigation' and url = '/friend'");
    execute(
        connection,
        "update menus set title = '友情链接' where type = 'footer_navigation' and url = '/links' and title in ('友链', '友链申请')");

    long writingId = ensureMenu(connection, "header_navigation", null, "写作", "/blog", "ri-quill-pen-line", 1);
    ensureMenu(connection, "header_navigation", writingId, "文章列表", "/blog", "ri-article-line", 1);
    ensureMenu(connection, "header_navigation", writingId, "分类", "/categories", "ri-folder-line", 2);
    ensureMenu(connection, "header_navigation", writingId, "标签", "/tags", "ri-price-tag-3-line", 3);
    ensureMenu(connection, "header_navigation", writingId, "归档", "/archive", "ri-archive-line", 4);
    ensureMenu(connection, "header_navigation", null, "相册", "/gallery", "ri-gallery-line", 2);
    ensureMenu(connection, "header_navigation", null, "瞬间", "/moments", "ri-bubble-chart-line", 3);
    ensureMenu(connection, "header_navigation", null, "关于", "/about", "ri-information-line", 4);
  }

  private Map<String, String> loadGroup(Connection connection, String group) throws SQLException {
    Map<String, String> values = new LinkedHashMap<>();
    try (PreparedStatement statement =
            connection.prepareStatement(
                "select key_name, value_text from settings where group_name = ? order by key_name")) {
      statement.setString(1, group);
      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          values.put(resultSet.getString("key_name"), resultSet.getString("value_text"));
        }
      }
    }
    return values;
  }

  private void upsertIfBlank(Connection connection, String group, String key, String value) throws SQLException {
    String normalized = value == null ? "" : value;
    try (PreparedStatement update =
            connection.prepareStatement(
                """
                update settings
                set value_text = ?, updated_at = current_timestamp
                where group_name = ? and key_name = ? and coalesce(trim(value_text), '') = ''
                """)) {
      update.setString(1, normalized);
      update.setString(2, group);
      update.setString(3, key);
      int updated = update.executeUpdate();
      if (updated > 0) {
        return;
      }
    }

    try (PreparedStatement insert =
            connection.prepareStatement(
                """
                insert into settings (group_name, key_name, value_text)
                select ?, ?, ?
                where not exists (
                  select 1 from settings where group_name = ? and key_name = ?
                )
                """)) {
      insert.setString(1, group);
      insert.setString(2, key);
      insert.setString(3, normalized);
      insert.setString(4, group);
      insert.setString(5, key);
      insert.executeUpdate();
    }
  }

  private long ensureMenu(
      Connection connection, String type, Long parentId, String title, String url, String icon, int sort)
      throws SQLException {
    Long existingId = findMenuId(connection, type, parentId, title, url);
    if (existingId != null) {
      try (PreparedStatement update =
              connection.prepareStatement(
                  """
                  update menus
                  set title = ?, url = ?, icon = ?, sort_order = ?
                  where id = ?
                  """)) {
        update.setString(1, title);
        update.setString(2, url);
        update.setString(3, icon);
        update.setInt(4, sort);
        update.setLong(5, existingId);
        update.executeUpdate();
      }
      return existingId;
    }

    try (PreparedStatement insert =
            connection.prepareStatement(
                "insert into menus (type, parent_id, title, url, icon, sort_order) values (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
      insert.setString(1, type);
      if (parentId == null) {
        insert.setNull(2, Types.BIGINT);
      } else {
        insert.setLong(2, parentId);
      }
      insert.setString(3, title);
      insert.setString(4, url);
      insert.setString(5, icon);
      insert.setInt(6, sort);
      insert.executeUpdate();

      try (ResultSet keys = insert.getGeneratedKeys()) {
        if (keys.next()) {
          return keys.getLong(1);
        }
      }
    }

    throw new SQLException("Failed to create menu: " + title);
  }

  private Long findMenuId(Connection connection, String type, Long parentId, String title, String url)
      throws SQLException {
    String sql =
        parentId == null
            ? """
              select id
              from menus
              where type = ?
                and parent_id is null
                and title = ?
                and url = ?
              order by id
              limit 1
              """
            : """
              select id
              from menus
              where type = ?
                and parent_id = ?
                and title = ?
                and url = ?
              order by id
              limit 1
              """;
    try (PreparedStatement statement =
            connection.prepareStatement(sql)) {
      statement.setString(1, type);
      int nextIndex = 2;
      if (parentId != null) {
        statement.setLong(nextIndex++, parentId);
      }
      statement.setString(nextIndex++, title);
      statement.setString(nextIndex, url);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getLong("id") : null;
      }
    }
  }

  private void execute(Connection connection, String sql) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
    }
  }

  private String firstNonBlank(Map<String, String> primary, Map<String, String> secondary, String... keys) {
    String value = firstNonBlank(primary, keys);
    return value.isBlank() ? firstNonBlank(secondary, keys) : value;
  }

  private String firstNonBlank(Map<String, String> values, String... keys) {
    for (String key : keys) {
      String value = read(values, key);
      if (!value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String read(Map<String, String> values, String key) {
    String direct = values.get(key);
    if (direct != null) {
      return direct;
    }

    int dotIndex = key.indexOf('.');
    if (dotIndex >= 0) {
      String shortKey = key.substring(dotIndex + 1);
      String shortValue = values.get(shortKey);
      if (shortValue != null) {
        return shortValue;
      }
    }

    for (String prefix : List.of("basic.", "blog.")) {
      String prefixed = values.get(prefix + key);
      if (prefixed != null) {
        return prefixed;
      }
    }

    return "";
  }

  private List<Map<String, Object>> readList(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    try {
      List<Map<String, Object>> parsed = objectMapper.readValue(raw, LIST_OF_MAPS);
      return parsed == null ? List.of() : parsed;
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private List<Map<String, Object>> migrateStatusItems(String raw) {
    List<Map<String, Object>> profileItems = readList(raw);
    List<Map<String, Object>> result = new ArrayList<>();
    for (int index = 0; index < Math.min(3, profileItems.size()); index++) {
      Map<String, Object> source = profileItems.get(index);
      String label = text(source.get("label"));
      String content = text(source.get("value"));
      if (label.isBlank() || content.isBlank()) {
        continue;
      }

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("icon", "");
      item.put("label", label);
      item.put("content", content);
      item.put("sort", index + 1);
      result.add(item);
    }
    return result;
  }

  private List<Map<String, Object>> migrateSkillItems(String raw) {
    List<Map<String, Object>> profileItems = readList(raw);
    List<Map<String, Object>> result = new ArrayList<>();
    int sort = 1;
    for (int index = 3; index < profileItems.size(); index++) {
      Map<String, Object> source = profileItems.get(index);
      String name = text(source.get("label"));
      String value = text(source.get("value"));
      if (name.isBlank() || value.isBlank() || !isNumeric(value)) {
        continue;
      }

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("name", name);
      item.put("value", value);
      item.put("sort", sort++);
      result.add(item);
    }
    return result;
  }

  private List<Map<String, Object>> migrateTimelineItems(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    List<Map<String, Object>> fromJson = readList(raw);
    List<Map<String, Object>> normalizedJson = new ArrayList<>();
    int jsonSort = 1;
    for (Map<String, Object> item : fromJson) {
      String year = text(item.get("year"));
      String event = text(item.get("event"));
      if (year.isBlank() || event.isBlank()) {
        continue;
      }

      Map<String, Object> normalized = new LinkedHashMap<>();
      normalized.put("year", year);
      normalized.put("event", event);
      normalized.put("sort", jsonSort++);
      normalizedJson.add(normalized);
    }
    if (!normalizedJson.isEmpty()) {
      return normalizedJson;
    }

    String[] lines = raw.split("\\R");
    List<Map<String, Object>> result = new ArrayList<>();
    int sort = 1;
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isBlank()) {
        continue;
      }
      String[] parts = trimmed.split("[:：]", 2);
      if (parts.length < 2) {
        return List.of();
      }

      String year = parts[0].trim();
      String event = parts[1].trim();
      if (year.isBlank() || event.isBlank()) {
        return List.of();
      }

      Map<String, Object> item = new LinkedHashMap<>();
      item.put("year", year);
      item.put("event", event);
      item.put("sort", sort++);
      result.add(item);
    }
    return result;
  }

  private String migrateBottomQuote(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }

    try {
      List<String> lines = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
      return lines.stream().map(String::trim).filter(line -> !line.isBlank()).reduce((left, right) -> left + " " + right).orElse("");
    } catch (Exception ignored) {
      return "";
    }
  }

  private List<Map<String, Object>> migrateSocialLinks(String raw) {
    List<Map<String, Object>> source = readList(raw);
    List<Map<String, Object>> result = new ArrayList<>();
    int index = 1;
    for (Map<String, Object> item : source) {
      String name = text(item.get("name"));
      String url = text(item.get("url"));
      if (name.isBlank() || url.isBlank()) {
        continue;
      }

      Map<String, Object> normalized = new LinkedHashMap<>();
      normalized.put("icon", text(item.get("icon")));
      normalized.put("name", name);
      normalized.put("url", normalizeMenuUrl(url));
      normalized.put("sort", number(item.get("sort"), index));
      result.add(normalized);
      index++;
    }
    return result;
  }

  private String normalizeMenuUrl(String url) {
    return switch (url) {
      case "/album" -> "/gallery";
      case "/moment" -> "/moments";
      case "/message" -> "/guestbook";
      case "/statistics" -> "/stats";
      case "/friend" -> "/links";
      default -> url;
    };
  }

  private String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  private boolean isNumeric(String value) {
    try {
      Double.parseDouble(value);
      return true;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  private int number(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }

    if (value instanceof String text) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException ignored) {
        return fallback;
      }
    }

    return fallback;
  }
}
