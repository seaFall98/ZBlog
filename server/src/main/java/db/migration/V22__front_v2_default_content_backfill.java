package db.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V22__front_v2_default_content_backfill extends BaseJavaMigration {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();

    upsertIfBlank(connection, "v2_home", "hero_eyebrow", "个人出版物");
    upsertIfBlank(connection, "v2_home", "hero_title", "以文字作舟\n渡光阴\n之河");
    upsertIfBlank(
        connection,
        "v2_home",
        "hero_meta",
        "在文章、相册与瞬间之间，慢慢写，慢慢记。");
    upsertIfBlank(connection, "v2_home", "hero_cta_label", "阅读文章");
    upsertIfBlank(connection, "v2_home", "hero_cta_target", "/blog");

    upsertIfBlank(
        connection,
        "v2_about",
        "intro_text",
        "我喜欢安静地记录日常，也喜欢把一些不急着说完的话，慢慢写下来。\n\n这不是一份宏大的自我介绍，更像一张留给未来自己的书签。");
    upsertIfBlank(
        connection,
        "v2_about",
        "status_items",
        objectMapper.writeValueAsString(
            List.of(
                Map.of("icon", "book-open", "label", "正在读", "content", "百年孤独", "sort", 1),
                Map.of("icon", "camera", "label", "最近在拍", "content", "秋日街景", "sort", 2),
                Map.of("icon", "heart", "label", "最近喜欢", "content", "煮咖啡", "sort", 3))));
    upsertIfBlank(
        connection,
        "v2_about",
        "skill_items",
        objectMapper.writeValueAsString(
            List.of(
                Map.of("name", "写作", "value", "90", "sort", 1),
                Map.of("name", "摄影", "value", "75", "sort", 2),
                Map.of("name", "阅读", "value", "95", "sort", 3),
                Map.of("name", "旅行", "value", "80", "sort", 4))));
    upsertIfBlank(
        connection,
        "v2_about",
        "timeline_items",
        objectMapper.writeValueAsString(
            List.of(
                Map.of("year", "2024", "event", "开始认真记录，保持稳定更新", "sort", 1),
                Map.of("year", "2023", "event", "第一次独自远行，重新理解慢下来", "sort", 2),
                Map.of("year", "2022", "event", "买下第一台相机，开始有意识地拍照", "sort", 3),
                Map.of("year", "2021", "event", "写下第一篇真正想留下来的文章", "sort", 4))));
    upsertIfBlank(
        connection,
        "v2_about",
        "bottom_quote",
        "生活就是很多很多个平凡的日子，偶尔有一些光。");

    upsertIfBlank(connection, "v2_guestbook", "intro_text", "把想说的话留在这里，让它慢慢飘过留言墙。");

    upsertIfBlank(
        connection,
        "v2_footer",
        "description",
        "记录平凡生活里的光与影，写作是一种安静的对话。");
    upsertIfBlank(connection, "v2_footer", "copyright_text", "© 2026 寂静之书");
    upsertIfBlank(connection, "v2_footer", "slogan", "以文字作舟，渡光阴之河");

    seedFooterMenus(connection);
  }

  private void seedFooterMenus(Connection connection) throws SQLException {
    long navigationId =
        ensureMenu(connection, "footer_navigation", null, "导航", "", "ri-compass-3-line", 1);
    ensureMenu(connection, "footer_navigation", navigationId, "首页", "/", "ri-home-4-line", 1);
    ensureMenu(connection, "footer_navigation", navigationId, "分类", "/categories", "ri-folder-line", 2);
    ensureMenu(connection, "footer_navigation", navigationId, "标签", "/tags", "ri-price-tag-3-line", 3);
    ensureMenu(connection, "footer_navigation", navigationId, "归档", "/archive", "ri-archive-line", 4);

    long protocolId =
        ensureMenu(connection, "footer_navigation", null, "协议", "", "ri-file-list-3-line", 2);
    ensureMenu(connection, "footer_navigation", protocolId, "隐私政策", "/privacy", "ri-shield-keyhole-line", 1);
    ensureMenu(connection, "footer_navigation", protocolId, "Cookies", "/cookies", "ri-cookie-line", 2);
    ensureMenu(connection, "footer_navigation", protocolId, "版权声明", "/copyright", "ri-copyright-line", 3);

    long aboutId =
        ensureMenu(connection, "footer_navigation", null, "关于", "", "ri-information-line", 3);
    ensureMenu(connection, "footer_navigation", aboutId, "关于博客", "/about", "ri-information-line", 1);
    ensureMenu(connection, "footer_navigation", aboutId, "留言板", "/guestbook", "ri-chat-1-line", 2);
    ensureMenu(connection, "footer_navigation", aboutId, "友情链接", "/links", "ri-links-line", 3);
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
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
}
