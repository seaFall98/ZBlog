package com.zblog.identity.infrastructure;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.identity.application.AdminUserQuery;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.identity.domain.UserAccount;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class JdbcUserRepository implements UserRepository {

  private final JdbcClient jdbcClient;
  private final JdbcTemplate jdbcTemplate;

  public JdbcUserRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
    this.jdbcClient = jdbcClient;
    this.jdbcTemplate = jdbcTemplate;
  }

  public UserAccount findByEmail(String email) {
    List<UserAccount> users =
        jdbcClient
            .sql(
                """
                select id, email, password_hash, nickname, avatar, badge, website, role, is_enabled,
                  deleted_at, last_login, github_id, google_id, qq_id, microsoft_id, feishu_open_id,
                  created_at, updated_at
                from users
                where email = :email
                """)
            .param("email", email)
            .query(this::mapRow)
            .list();
    if (users.isEmpty()) {
      throw new BusinessException(401, "Invalid username or password", HttpStatus.UNAUTHORIZED);
    }
    return users.getFirst();
  }

  public UserAccount findById(long id) {
    List<UserAccount> users =
        jdbcClient
            .sql(
                """
                select id, email, password_hash, nickname, avatar, badge, website, role, is_enabled,
                  deleted_at, last_login, github_id, google_id, qq_id, microsoft_id, feishu_open_id,
                  created_at, updated_at
                from users
                where id = :id
                """)
            .param("id", id)
            .query(this::mapRow)
            .list();
    if (users.isEmpty()) {
      throw new BusinessException(404, "User not found", HttpStatus.NOT_FOUND);
    }
    return users.getFirst();
  }

  public boolean existsByEmail(String email) {
    Integer count = jdbcTemplate.queryForObject("select count(*) from users where email = ?", Integer.class, email);
    return count != null && count > 0;
  }

  public UserAccount create(
      String email,
      String passwordHash,
      String nickname,
      String role,
      String avatar,
      String badge,
      String website,
      boolean enabled) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                  insert into users (email, password_hash, nickname, avatar, badge, website, role, is_enabled)
                  values (?, ?, ?, ?, ?, ?, ?, ?)
                  """,
                  new String[] {"id"});
          statement.setString(1, email);
          statement.setString(2, passwordHash);
          statement.setString(3, nickname);
          statement.setString(4, avatar == null ? "" : avatar);
          statement.setString(5, badge);
          statement.setString(6, website);
          statement.setString(7, role);
          statement.setBoolean(8, enabled);
          return statement;
        },
        keyHolder);
    return findById(keyHolder.getKey().longValue());
  }

  public void updateLastLogin(long id) {
    jdbcTemplate.update("update users set last_login = current_timestamp where id = ?", id);
  }

  public void updateProfile(long id, String email, String nickname, String avatar, String badge, String website) {
    jdbcTemplate.update(
        """
        update users
        set email = ?, nickname = ?, avatar = ?, badge = ?, website = ?, updated_at = current_timestamp
        where id = ?
        """,
        email,
        nickname,
        avatar,
        badge,
        website,
        id);
  }

  public void updatePassword(long id, String passwordHash) {
    jdbcTemplate.update(
        "update users set password_hash = ?, updated_at = current_timestamp where id = ?", passwordHash, id);
  }

  public void softDelete(long id) {
    jdbcTemplate.update("update users set deleted_at = current_timestamp, is_enabled = false where id = ?", id);
  }

  public void clearOauthProvider(long id, String column) {
    jdbcTemplate.update("update users set " + column + " = '', updated_at = current_timestamp where id = ?", id);
  }

  public PageResponse<Map<String, Object>> listAdmin(AdminUserQuery query) {
    Map<String, String> params = query.params();
    int page = parsePositive(params.get("page"), 1);
    int pageSize = parsePositive(params.get("page_size"), 20);
    List<Object> args = new ArrayList<>();
    String where = buildWhere(params, args);
    long total = jdbcTemplate.queryForObject("select count(*) from users " + where, Long.class, args.toArray());
    args.add(pageSize);
    args.add((page - 1) * pageSize);
    List<Map<String, Object>> list =
        jdbcTemplate.query(
            """
            select id, email, password_hash, nickname, avatar, badge, website, role, is_enabled,
              deleted_at, last_login, github_id, google_id, qq_id, microsoft_id, feishu_open_id,
              created_at, updated_at
            from users
            """
                + where
                + " order by id desc limit ? offset ?",
            (rs, rowNum) -> UserAccountMapper.toUserMap(mapRow(rs, rowNum), false),
            args.toArray());
    return new PageResponse<>(list, total, page, pageSize);
  }

  public void updateAdmin(
      long id,
      String email,
      String nickname,
      String avatar,
      String badge,
      String website,
      String role,
      boolean enabled) {
    jdbcTemplate.update(
        """
        update users
        set email = ?, nickname = ?, avatar = ?, badge = ?, website = ?, role = ?, is_enabled = ?, updated_at = current_timestamp
        where id = ?
        """,
        email,
        nickname,
        avatar,
        badge,
        website,
        role,
        enabled,
        id);
  }

  private String buildWhere(Map<String, String> params, List<Object> args) {
    List<String> conditions = new ArrayList<>();
    if (!Boolean.parseBoolean(params.getOrDefault("is_deleted", "false"))) {
      conditions.add("deleted_at is null");
    } else {
      conditions.add("deleted_at is not null");
    }
    if (StringUtils.hasText(params.get("keyword"))) {
      conditions.add("(email like ? or nickname like ?)");
      String keyword = "%" + params.get("keyword") + "%";
      args.add(keyword);
      args.add(keyword);
    }
    if (StringUtils.hasText(params.get("role"))) {
      conditions.add("role = ?");
      args.add(params.get("role"));
    }
    if (StringUtils.hasText(params.get("is_enabled"))) {
      conditions.add("is_enabled = ?");
      args.add(Boolean.parseBoolean(params.get("is_enabled")));
    }
    if (StringUtils.hasText(params.get("login_method"))) {
      switch (params.get("login_method")) {
        case "password" -> conditions.add("password_hash is not null and password_hash <> ''");
        case "github" -> conditions.add("github_id <> ''");
        case "google" -> conditions.add("google_id <> ''");
        case "qq" -> conditions.add("qq_id <> ''");
        case "microsoft" -> conditions.add("microsoft_id <> ''");
        default -> {}
      }
    }
    return conditions.isEmpty() ? "" : "where " + String.join(" and ", conditions);
  }

  private int parsePositive(String raw, int fallback) {
    try {
      int value = Integer.parseInt(raw);
      return value > 0 ? value : fallback;
    } catch (Exception exception) {
      return fallback;
    }
  }

  private UserAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new UserAccount(
        rs.getLong("id"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("nickname"),
        rs.getString("avatar"),
        rs.getString("badge"),
        rs.getString("website"),
        rs.getString("role"),
        rs.getBoolean("is_enabled"),
        instant(rs.getTimestamp("deleted_at")),
        instant(rs.getTimestamp("last_login")),
        rs.getString("github_id"),
        rs.getString("google_id"),
        rs.getString("qq_id"),
        rs.getString("microsoft_id"),
        rs.getString("feishu_open_id"),
        instant(rs.getTimestamp("created_at")),
        instant(rs.getTimestamp("updated_at")));
  }

  private java.time.Instant instant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
