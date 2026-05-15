package com.zblog.identity;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.config.SecurityProperties;
import com.zblog.security.JwtService;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

  private final JdbcClient jdbcClient;
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SecurityProperties securityProperties;

  public UserService(
      JdbcClient jdbcClient,
      JdbcTemplate jdbcTemplate,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      SecurityProperties securityProperties) {
    this.jdbcClient = jdbcClient;
    this.jdbcTemplate = jdbcTemplate;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.securityProperties = securityProperties;
  }

  public LoginResponse login(String username, String password) {
    UserRecord user = findByEmail(username);
    if (!user.isEnabled() || user.deletedAt() != null) {
      throw new BusinessException(401, "User is disabled", HttpStatus.UNAUTHORIZED);
    }
    if (!StringUtils.hasText(user.passwordHash())
        || !passwordEncoder.matches(password, user.passwordHash())) {
      throw new BusinessException(401, "Invalid username or password", HttpStatus.UNAUTHORIZED);
    }
    jdbcTemplate.update("update users set last_login = current_timestamp where id = ?", user.id());
    return tokenResponse(findById(user.id()));
  }

  public LoginResponse register(Map<String, Object> request) {
    String email = requiredString(request, "email");
    String nickname = requiredString(request, "nickname");
    String password = requiredString(request, "password");
    String website = stringValue(request.get("website"));
    if (existsByEmail(email)) {
      throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
    }
    UserRecord user = createUser(email, password, nickname, "user", "", null, website, true);
    return tokenResponse(user);
  }

  public LoginResponse refresh(String email) {
    return tokenResponse(findActiveByEmail(email));
  }

  public Map<String, Object> profile(String email) {
    return toUserMap(findActiveByEmail(email), true);
  }

  public Map<String, Object> updateProfile(String email, Map<String, Object> request) {
    UserRecord user = findActiveByEmail(email);
    String nickname = optionalString(request, "nickname", user.nickname());
    String newEmail = optionalString(request, "email", user.email());
    String avatar = optionalString(request, "avatar", user.avatar());
    String badge = nullableString(request.get("badge"));
    String website = nullableString(request.get("website"));
    if (!newEmail.equals(user.email()) && existsByEmail(newEmail)) {
      throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
    }
    jdbcTemplate.update(
        """
        update users
        set email = ?, nickname = ?, avatar = ?, badge = ?, website = ?, updated_at = current_timestamp
        where id = ?
        """,
        newEmail,
        nickname,
        avatar,
        badge,
        website,
        user.id());
    return toUserMap(findById(user.id()), true);
  }

  public void changePassword(String email, Map<String, Object> request) {
    UserRecord user = findActiveByEmail(email);
    String oldPassword = requiredString(request, "old_password");
    String newPassword = requiredString(request, "new_password");
    if (!StringUtils.hasText(user.passwordHash())
        || !passwordEncoder.matches(oldPassword, user.passwordHash())) {
      throw new BusinessException(400, "旧密码错误", HttpStatus.BAD_REQUEST);
    }
    updatePassword(user.id(), newPassword);
  }

  public void setPassword(String email, Map<String, Object> request) {
    UserRecord user = findActiveByEmail(email);
    if (StringUtils.hasText(user.passwordHash())) {
      throw new BusinessException(400, "Password already set", HttpStatus.BAD_REQUEST);
    }
    updatePassword(user.id(), requiredString(request, "password"));
  }

  public void deactivate(String email, Map<String, Object> request) {
    UserRecord user = findActiveByEmail(email);
    String password = requiredString(request, "password");
    if (!StringUtils.hasText(user.passwordHash())
        || !passwordEncoder.matches(password, user.passwordHash())) {
      throw new BusinessException(400, "密码错误", HttpStatus.BAD_REQUEST);
    }
    jdbcTemplate.update(
        "update users set deleted_at = current_timestamp, is_enabled = false where id = ?", user.id());
  }

  public void unbindOAuth(String email, String provider) {
    UserRecord user = findActiveByEmail(email);
    String column = oauthColumn(provider);
    int loginMethods = loginMethodCount(user);
    if (loginMethods <= 1) {
      throw new BusinessException(400, "至少保留一种登录方式", HttpStatus.BAD_REQUEST);
    }
    jdbcTemplate.update("update users set " + column + " = '', updated_at = current_timestamp where id = ?", user.id());
  }

  public PageResponse<Map<String, Object>> listAdmin(Map<String, String> params) {
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
            (rs, rowNum) -> toUserMap(mapRow(rs, rowNum), false),
            args.toArray());
    return new PageResponse<>(list, total, page, pageSize);
  }

  public Map<String, Object> getAdmin(long id) {
    return toUserMap(findById(id), false);
  }

  public Map<String, Object> createAdmin(Map<String, Object> request) {
    String email = requiredString(request, "email");
    if (existsByEmail(email)) {
      throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
    }
    UserRecord user =
        createUser(
            email,
            requiredString(request, "password"),
            requiredString(request, "nickname"),
            optionalString(request, "role", "user"),
            optionalString(request, "avatar", ""),
            nullableString(request.get("badge")),
            nullableString(request.get("website")),
            true);
    return toUserMap(user, false);
  }

  public Map<String, Object> updateAdmin(long id, Map<String, Object> request) {
    UserRecord user = findById(id);
    String email = optionalString(request, "email", user.email());
    if (!email.equals(user.email()) && existsByEmail(email)) {
      throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
    }
    jdbcTemplate.update(
        """
        update users
        set email = ?, nickname = ?, avatar = ?, badge = ?, website = ?, role = ?, is_enabled = ?, updated_at = current_timestamp
        where id = ?
        """,
        email,
        optionalString(request, "nickname", user.nickname()),
        optionalString(request, "avatar", user.avatar()),
        nullableOrExisting(request, "badge", user.badge()),
        nullableOrExisting(request, "website", user.website()),
        optionalString(request, "role", user.role()),
        optionalBoolean(request, "is_enabled", user.isEnabled()),
        id);
    if (StringUtils.hasText(stringValue(request.get("password")))) {
      updatePassword(id, stringValue(request.get("password")));
    }
    return toUserMap(findById(id), false);
  }

  public void deleteAdmin(long id) {
    UserRecord user = findById(id);
    if ("super_admin".equals(user.role())) {
      throw new BusinessException(400, "Cannot delete super admin", HttpStatus.BAD_REQUEST);
    }
    jdbcTemplate.update(
        "update users set deleted_at = current_timestamp, is_enabled = false where id = ?", id);
  }

  public void resetPasswordAdmin(long id, Map<String, Object> request) {
    findById(id);
    updatePassword(id, requiredString(request, "new_password"));
  }

  private UserRecord createUser(
      String email,
      String password,
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
          statement.setString(2, passwordEncoder.encode(password));
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

  private LoginResponse tokenResponse(UserRecord user) {
    return new LoginResponse(
        jwtService.createAdminToken(user.email()),
        "Bearer",
        securityProperties.getTokenTtlMinutes() * 60,
        toUserMap(user, true));
  }

  private void updatePassword(long id, String password) {
    jdbcTemplate.update(
        "update users set password_hash = ?, updated_at = current_timestamp where id = ?",
        passwordEncoder.encode(password),
        id);
  }

  private UserRecord findActiveByEmail(String email) {
    UserRecord user = findByEmail(email);
    if (!user.isEnabled() || user.deletedAt() != null) {
      throw new BusinessException(401, "User is disabled", HttpStatus.UNAUTHORIZED);
    }
    return user;
  }

  private UserRecord findByEmail(String email) {
    List<UserRecord> users =
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

  private UserRecord findById(long id) {
    List<UserRecord> users =
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

  private boolean existsByEmail(String email) {
    Integer count = jdbcTemplate.queryForObject("select count(*) from users where email = ?", Integer.class, email);
    return count != null && count > 0;
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

  private Map<String, Object> toUserMap(UserRecord user, boolean includeLinkedOauths) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", user.id());
    map.put("email", user.email());
    map.put("email_hash", "");
    map.put("is_virtual_email", false);
    map.put("nickname", user.nickname());
    map.put("avatar", user.avatar());
    map.put("badge", user.badge());
    map.put("website", user.website());
    map.put("role", user.role());
    map.put("is_enabled", user.isEnabled());
    map.put("deleted_at", format(user.deletedAt()));
    map.put("last_login", format(user.lastLogin()));
    map.put("created_at", format(user.createdAt()));
    map.put("has_password", StringUtils.hasText(user.passwordHash()));
    map.put("github_id", user.githubId());
    map.put("google_id", user.googleId());
    map.put("qq_id", user.qqId());
    map.put("microsoft_id", user.microsoftId());
    map.put("feishu_open_id", user.feishuOpenId());
    if (includeLinkedOauths) {
      List<String> linkedOauths = new ArrayList<>();
      if (StringUtils.hasText(user.githubId())) linkedOauths.add("github");
      if (StringUtils.hasText(user.googleId())) linkedOauths.add("google");
      if (StringUtils.hasText(user.qqId())) linkedOauths.add("qq");
      if (StringUtils.hasText(user.microsoftId())) linkedOauths.add("microsoft");
      map.put("linked_oauths", linkedOauths);
    }
    return map;
  }

  private UserRecord mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
    return new UserRecord(
        rs.getLong("id"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("nickname"),
        rs.getString("avatar"),
        rs.getString("badge"),
        rs.getString("website"),
        rs.getString("role"),
        rs.getBoolean("is_enabled"),
        rs.getTimestamp("deleted_at"),
        rs.getTimestamp("last_login"),
        rs.getString("github_id"),
        rs.getString("google_id"),
        rs.getString("qq_id"),
        rs.getString("microsoft_id"),
        rs.getString("feishu_open_id"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at"));
  }

  private String requiredString(Map<String, Object> request, String key) {
    String value = stringValue(request.get(key));
    if (!StringUtils.hasText(value)) {
      throw new BusinessException(400, key + " is required", HttpStatus.BAD_REQUEST);
    }
    return value.trim();
  }

  private String optionalString(Map<String, Object> request, String key, String fallback) {
    String value = stringValue(request.get(key));
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }

  private Boolean optionalBoolean(Map<String, Object> request, String key, boolean fallback) {
    Object value = request.get(key);
    return value instanceof Boolean booleanValue ? booleanValue : fallback;
  }

  private String nullableOrExisting(Map<String, Object> request, String key, String fallback) {
    return request.containsKey(key) ? nullableString(request.get(key)) : fallback;
  }

  private String stringValue(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private String nullableString(Object value) {
    String string = stringValue(value).trim();
    return string.isEmpty() ? null : string;
  }

  private int loginMethodCount(UserRecord user) {
    int count = 0;
    if (StringUtils.hasText(user.passwordHash())) count++;
    if (StringUtils.hasText(user.githubId())) count++;
    if (StringUtils.hasText(user.googleId())) count++;
    if (StringUtils.hasText(user.qqId())) count++;
    if (StringUtils.hasText(user.microsoftId())) count++;
    return count;
  }

  private String oauthColumn(String provider) {
    return switch (provider) {
      case "github" -> "github_id";
      case "google" -> "google_id";
      case "qq" -> "qq_id";
      case "microsoft" -> "microsoft_id";
      default -> throw new BusinessException(400, "Unsupported OAuth provider", HttpStatus.BAD_REQUEST);
    };
  }

  private int parsePositive(String raw, int fallback) {
    try {
      int value = Integer.parseInt(raw);
      return value > 0 ? value : fallback;
    } catch (Exception exception) {
      return fallback;
    }
  }

  private String format(Timestamp timestamp) {
    return timestamp == null
        ? null
        : OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC).toString();
  }

  private record UserRecord(
      long id,
      String email,
      String passwordHash,
      String nickname,
      String avatar,
      String badge,
      String website,
      String role,
      boolean isEnabled,
      Timestamp deletedAt,
      Timestamp lastLogin,
      String githubId,
      String googleId,
      String qqId,
      String microsoftId,
      String feishuOpenId,
      Timestamp createdAt,
      Timestamp updatedAt) {}
}
