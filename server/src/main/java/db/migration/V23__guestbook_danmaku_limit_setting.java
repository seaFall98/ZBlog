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

    // Only insert if the setting key does not already exist
    try (PreparedStatement check = connection.prepareStatement(
        "select count(*) from settings where setting_key = ?")) {
      check.setString(1, "v2_guestbook.danmaku_public_limit");
      try (ResultSet rs = check.executeQuery()) {
        if (rs.next() && rs.getInt(1) > 0) return;
      }
    }

    try (PreparedStatement insert = connection.prepareStatement(
        "insert into settings (setting_key, setting_value, created_at, updated_at) values (?, ?, now(), now())")) {
      insert.setString(1, "v2_guestbook.danmaku_public_limit");
      insert.setString(2, DEFAULT_LIMIT);
      insert.executeUpdate();
    }
  }
}
