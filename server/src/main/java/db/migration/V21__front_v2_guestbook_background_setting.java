package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V21__front_v2_guestbook_background_setting extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    Map<String, String> basic = loadGroup(connection, "basic");
    Map<String, String> blog = loadGroup(connection, "blog");

    upsertIfBlank(
        connection,
        "v2_guestbook",
        "background_image",
        firstNonBlank(blog, basic, "barrage_background_image", "background_image"));
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

  private void upsertIfBlank(Connection connection, String group, String key, String value)
      throws SQLException {
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

    for (String prefix : List.of("basic.", "blog.")) {
      String prefixed = values.get(prefix + key);
      if (prefixed != null) {
        return prefixed;
      }
    }

    return "";
  }
}
