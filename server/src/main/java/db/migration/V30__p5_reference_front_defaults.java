package db.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V30__p5_reference_front_defaults extends BaseJavaMigration {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();

    upsertIfBlankOrKnownDefault(connection, "v2_identity", "site_title", "寂静之书", "ZBlog");
    upsertIfBlankOrKnownDefault(connection, "v2_identity", "owner_display_name", "Z", "seaFall", "寂静");
    upsertIfBlankOrKnownDefault(
        connection, "v2_identity", "email", "zz1362410372@gmail.com", "hello@example.com", "hello@quietbook.me");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_identity",
        "primary_image_url",
        "/uploads/1782184837997_0f0990627d6a8491243fb20c34c097c2.png",
        "/avatar.webp",
        "/bg.webp",
        "https://images.unsplash.com/photo-1529665253569-6d01c0eaf7b6?w=600&q=80");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_identity",
        "favicon_url",
        "/uploads/1782184837996_ChatGPT_Image_May_19__2026__02_05_47_AM.png",
        "/favicon.ico");

    upsertIfBlankOrKnownDefault(connection, "v2_home", "hero_eyebrow", "Z的小站", "个人出版物");
    upsertIfBlankOrKnownDefault(connection, "v2_home", "hero_title", "以文字作舟\\n渡光阴\\n之河");
    upsertIfBlankOrKnownDefault(
        connection, "v2_home", "hero_meta", "在文章、相册与瞬间之间，慢慢写，慢慢记。");
    upsertIfBlankOrKnownDefault(connection, "v2_home", "hero_cta_label", "阅读文章");
    upsertIfBlankOrKnownDefault(connection, "v2_home", "hero_cta_target", "/blog");

    upsertIfBlankOrKnownDefault(
        connection,
        "v2_about",
        "intro_text",
        "一个喜欢在平凡生活里寻找微小美好的人。白天是个普通的上班族，晚上是个喜欢写字的人。\\n\\n"
            + "这个博客是我的私人空间，记录读书的感悟、旅途的光景、日常的碎碎念，以及那些一闪而过、如果不写下来就会忘记的瞬间。\\n\\n"
            + "相信文字有重量，相信好照片能留住时间，相信生活值得被认真对待。",
        "我喜欢安静地记录日常，也喜欢把一些不急着说完的话，慢慢写下来。\\n\\n这不是一份宏大的自我介绍，更像一张留给未来自己的书签。",
        "A builder who cares about product feel and technical quality.");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_about",
        "status_items",
        objectMapper.writeValueAsString(
            List.of(
                item("icon", "book-open", "label", "正在读", "content", "百年孤独", "sort", 1),
                item("icon", "camera", "label", "最近在拍", "content", "秋日街景", "sort", 2),
                item("icon", "heart", "label", "最近喜欢", "content", "煮咖啡", "sort", 3))),
        "[]");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_about",
        "skill_items",
        objectMapper.writeValueAsString(
            List.of(
                item("name", "写作", "value", "90", "sort", 1),
                item("name", "摄影", "value", "75", "sort", 2),
                item("name", "阅读", "value", "95", "sort", 3),
                item("name", "旅行", "value", "80", "sort", 4),
                item("name", "设计", "value", "65", "sort", 5))),
        objectMapper.writeValueAsString(
            List.of(
                item("name", "写作", "value", "90", "sort", 1),
                item("name", "摄影", "value", "75", "sort", 2),
                item("name", "阅读", "value", "95", "sort", 3),
                item("name", "旅行", "value", "80", "sort", 4))),
        "[]");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_about",
        "timeline_items",
        objectMapper.writeValueAsString(
            List.of(
                item("year", "2024", "event", "开始认真记录，坚持每周更新", "sort", 1),
                item("year", "2023", "event", "第一次独自旅行，去了日本京都", "sort", 2),
                item("year", "2022", "event", "买了第一台相机，开始认真学摄影", "sort", 3),
                item("year", "2021", "event", "建立这个博客，写下第一篇文章", "sort", 4))),
        objectMapper.writeValueAsString(
            List.of(
                item("year", "2024", "event", "开始认真记录，保持稳定更新", "sort", 1),
                item("year", "2023", "event", "第一次独自远行，重新理解慢下来", "sort", 2),
                item("year", "2022", "event", "买下第一台相机，开始有意识地拍照", "sort", 3),
                item("year", "2021", "event", "写下第一篇真正想留下来的文章", "sort", 4))),
        "[]");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_about",
        "bottom_quote",
        "生活就是很多很多个平凡的日子，偶尔有一些光。",
        "Keep building Keep learning",
        "[\"Keep building\",\"Keep learning\"]");

    upsertIfBlankOrKnownDefault(
        connection,
        "v2_guestbook",
        "intro_text",
        "欢迎留言~",
        "把想说的话留在这里，让它慢慢飘过留言墙。",
        "欢迎留言交流，分享你的想法或建议。");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_guestbook",
        "background_image",
        "/uploads/1782184837997_ChatGPT_Image_2026_6_12__06_00_01__4_.png",
        "/bg.webp");
    upsertIfBlankOrKnownDefault(connection, "v2_guestbook", "danmaku_public_limit", "300", "200");

    upsertIfBlankOrKnownDefault(
        connection,
        "v2_footer",
        "description",
        "记录平凡生活里的光与影，写作是一种安静的对话。",
        "A practical blog about development, products, and notes.",
        "Modern Blog");
    upsertIfBlankOrKnownDefault(
        connection, "v2_footer", "copyright_text", "© 2026 寂静之书", "© 2024 寂静之书");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_footer",
        "slogan",
        "以文字作舟，渡光阴之河",
        "Build in public, write with clarity.");
    upsertIfBlankOrKnownDefault(
        connection,
        "v2_footer",
        "social_links",
        objectMapper.writeValueAsString(
            List.of(
                item(
                    "icon",
                    "github-line",
                    "name",
                    "GitHub",
                    "url",
                    "https://github.com/seaFall98",
                    "sort",
                    1),
                item(
                    "icon",
                    "mail-line",
                    "name",
                    "Email",
                    "url",
                    "mailto:zz1362410372@gmail.com",
                    "sort",
                    2))),
        "[]",
        objectMapper.writeValueAsString(
            List.of(
                item(
                    "icon",
                    "github-line",
                    "name",
                    "GitHub",
                    "url",
                    "https://github.com/seaFall98",
                    "sort",
                    1),
                item(
                    "icon",
                    "mail-line",
                    "name",
                    "Email",
                    "url",
                    "mailto:hello@example.com",
                    "sort",
                    2))),
        objectMapper.writeValueAsString(
            List.of(
                item(
                    "icon",
                    "github-line",
                    "name",
                    "GitHub",
                    "url",
                    "https://github.com/seaFall98",
                    "sort",
                    1),
                item(
                    "icon",
                    "mail-line",
                    "name",
                    "Email",
                    "url",
                    "mailto:hello@quietbook.me",
                    "sort",
                    2))));

    backfillKnownDefaultLists(connection);
  }

  private void backfillKnownDefaultLists(Connection connection) throws SQLException {
    updateIfLooksLikeKnownDefault(
        connection,
        "v2_about",
        "status_items",
        objectMapperValue(
            List.of(
                item("icon", "book-open", "label", "正在读", "content", "百年孤独", "sort", 1),
                item("icon", "camera", "label", "最近在拍", "content", "秋日街景", "sort", 2),
                item("icon", "heart", "label", "最近喜欢", "content", "煮咖啡", "sort", 3))),
        List.of("Name", "Role", "Stack"),
        List.of("正在读"));

    updateIfLooksLikeKnownDefault(
        connection,
        "v2_about",
        "skill_items",
        objectMapperValue(
            List.of(
                item("name", "写作", "value", "90", "sort", 1),
                item("name", "摄影", "value", "75", "sort", 2),
                item("name", "阅读", "value", "95", "sort", 3),
                item("name", "旅行", "value", "80", "sort", 4),
                item("name", "设计", "value", "65", "sort", 5))),
        List.of("写作", "摄影", "阅读", "旅行"),
        List.of("设计"));

    updateIfLooksLikeKnownDefault(
        connection,
        "v2_about",
        "timeline_items",
        objectMapperValue(
            List.of(
                item("year", "2024", "event", "开始认真记录，坚持每周更新", "sort", 1),
                item("year", "2023", "event", "第一次独自旅行，去了日本京都", "sort", 2),
                item("year", "2022", "event", "买了第一台相机，开始认真学摄影", "sort", 3),
                item("year", "2021", "event", "建立这个博客，写下第一篇文章", "sort", 4))),
        List.of("开始认真记录，保持稳定更新", "第一次独自远行", "写下第一篇真正想留下来的文章"),
        List.of("第一次独自旅行，去了日本京都"));
  }

  private String objectMapperValue(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private Map<String, Object> item(Object... keyValues) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int index = 0; index < keyValues.length; index += 2) {
      map.put(keyValues[index].toString(), keyValues[index + 1]);
    }
    return map;
  }

  private void upsertIfBlankOrKnownDefault(
      Connection connection, String group, String key, String value, String... knownDefaults)
      throws SQLException {
    String normalized = value == null ? "" : value;
    List<String> candidates = new ArrayList<>();
    candidates.add("");
    for (String knownDefault : knownDefaults) {
      if (knownDefault != null) {
        candidates.add(knownDefault);
      }
    }

    String placeholders = String.join(", ", candidates.stream().map(ignored -> "?").toList());
    try (PreparedStatement update =
        connection.prepareStatement(
            """
            update settings
            set value_text = ?, updated_at = current_timestamp
            where group_name = ? and key_name = ?
              and coalesce(trim(value_text), '') in (
            """
                + placeholders
                + ")")) {
      update.setString(1, normalized);
      update.setString(2, group);
      update.setString(3, key);
      int index = 4;
      for (String candidate : candidates) {
        update.setString(index++, candidate);
      }
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

  private void updateIfLooksLikeKnownDefault(
      Connection connection,
      String group,
      String key,
      String value,
      List<String> requiredMarkers,
      List<String> forbiddenMarkers)
      throws SQLException {
    StringBuilder sql =
        new StringBuilder(
            """
            update settings
            set value_text = ?, updated_at = current_timestamp
            where group_name = ? and key_name = ?
              and coalesce(trim(value_text), '') <> ''
            """);
    for (int index = 0; index < requiredMarkers.size(); index++) {
      sql.append(" and value_text like ?");
    }
    for (int index = 0; index < forbiddenMarkers.size(); index++) {
      sql.append(" and value_text not like ?");
    }

    try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
      statement.setString(1, value);
      statement.setString(2, group);
      statement.setString(3, key);
      int index = 4;
      for (String marker : requiredMarkers) {
        statement.setString(index++, "%" + marker + "%");
      }
      for (String marker : forbiddenMarkers) {
        statement.setString(index++, "%" + marker + "%");
      }
      statement.executeUpdate();
    }
  }
}
