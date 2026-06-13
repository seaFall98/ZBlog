package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V23__guestbook_danmaku_limit_setting extends BaseJavaMigration {

  private static final String DEFAULT_LIMIT = "200";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();

    // Only insert if the setting does not already exist
    try (PreparedStatement check = connection.prepareStatement(
        "select count(*) from settings where group_name = ? and key_name = ?")) {
      check.setString(1, "v2_guestbook");
      check.setString(2, "danmaku_public_limit");
      try (ResultSet rs = check.executeQuery()) {
        if (rs.next() && rs.getInt(1) > 0) return;
      }
    }

    try (PreparedStatement insert = connection.prepareStatement(
        "insert into settings (group_name, key_name, value_text) values (?, ?, ?)")) {
      insert.setString(1, "v2_guestbook");
      insert.setString(2, "danmaku_public_limit");
      insert.setString(3, DEFAULT_LIMIT);
      insert.executeUpdate();
    }
  }
}
