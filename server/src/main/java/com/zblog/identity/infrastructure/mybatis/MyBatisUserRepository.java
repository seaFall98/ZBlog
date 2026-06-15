package com.zblog.identity.infrastructure.mybatis;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.identity.application.AdminUserQuery;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.identity.domain.UserAccount;
import com.zblog.identity.infrastructure.UserAccountMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MyBatisUserRepository implements UserRepository {

  private static final java.util.Set<String> OAUTH_COLUMNS =
      java.util.Set.of("github_id", "google_id", "qq_id", "microsoft_id", "feishu_open_id");

  private final UserMapper userMapper;

  public MyBatisUserRepository(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  public UserAccount findByEmail(String email) {
    return userMapper.rowsByEmail(email).stream()
        .findFirst()
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(401, "Invalid username or password", HttpStatus.UNAUTHORIZED));
  }

  public UserAccount findById(long id) {
    return userMapper.rowsById(id).stream()
        .findFirst()
        .map(this::mapRow)
        .orElseThrow(() -> new BusinessException(404, "User not found", HttpStatus.NOT_FOUND));
  }

  public boolean existsByEmail(String email) {
    return userMapper.countByEmail(email) > 0;
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
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("email", email);
    params.put("passwordHash", passwordHash);
    params.put("nickname", nickname);
    params.put("role", role);
    params.put("avatar", avatar == null ? "" : avatar);
    params.put("badge", badge);
    params.put("website", website);
    params.put("enabled", enabled);
    userMapper.insertUser(params);
    return findById(((Number) params.get("id")).longValue());
  }

  public void updateLastLogin(long id) {
    userMapper.updateLastLogin(id);
  }

  public void updateProfile(long id, String email, String nickname, String avatar, String badge, String website, String bio) {
    userMapper.updateProfile(id, email, nickname, avatar, badge, website, bio);
  }

  public void updatePassword(long id, String passwordHash) {
    userMapper.updatePassword(id, passwordHash);
  }

  public void softDelete(long id) {
    userMapper.softDelete(id);
  }

  public void clearOauthProvider(long id, String column) {
    if (!OAUTH_COLUMNS.contains(column)) {
      throw new BusinessException(40001, "Unsupported OAuth provider", HttpStatus.BAD_REQUEST);
    }
    userMapper.clearOauthProvider(id, column);
  }

  public PageResponse<Map<String, Object>> listAdmin(AdminUserQuery query) {
    Map<String, String> params = query.params();
    int page = parsePositive(params.get("page"), 1);
    int pageSize = parsePositive(params.get("page_size"), 20);
    boolean deleted = Boolean.parseBoolean(params.getOrDefault("is_deleted", "false"));
    String keyword = StringUtils.hasText(params.get("keyword")) ? "%" + params.get("keyword") + "%" : null;
    String role = StringUtils.hasText(params.get("role")) ? params.get("role") : null;
    Boolean enabled =
        StringUtils.hasText(params.get("is_enabled")) ? Boolean.parseBoolean(params.get("is_enabled")) : null;
    String loginMethod = StringUtils.hasText(params.get("login_method")) ? params.get("login_method") : null;
    long total = userMapper.countAdminRows(deleted, keyword, role, enabled, loginMethod);
    List<Map<String, Object>> list =
        userMapper.listAdminRows(deleted, keyword, role, enabled, loginMethod, pageSize, (page - 1) * pageSize).stream()
            .map(row -> UserAccountMapper.toUserMap(mapRow(row), false))
            .toList();
    return new PageResponse<>(list, total, page, pageSize);
  }

  public void updateAdmin(
      long id,
      String email,
      String nickname,
      String avatar,
      String badge,
      String website,
      String bio,
      String role,
      boolean enabled) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("id", id);
    params.put("email", email);
    params.put("nickname", nickname);
    params.put("avatar", avatar);
    params.put("badge", badge);
    params.put("website", website);
    params.put("bio", bio);
    params.put("role", role);
    params.put("enabled", enabled);
    userMapper.updateAdmin(params);
  }

  private int parsePositive(String raw, int fallback) {
    try {
      int value = Integer.parseInt(raw);
      return value > 0 ? value : fallback;
    } catch (Exception exception) {
      return fallback;
    }
  }

  private UserAccount mapRow(Map<String, Object> row) {
    return new UserAccount(
        number(row.get("id")),
        string(row.get("email")),
        string(row.get("password_hash")),
        string(row.get("nickname")),
        string(row.get("avatar")),
        string(row.get("badge")),
        string(row.get("website")),
        string(row.get("bio")),
        string(row.get("role")),
        bool(row.get("is_enabled")),
        instant(row.get("deleted_at")),
        instant(row.get("last_login")),
        string(row.get("github_id")),
        string(row.get("google_id")),
        string(row.get("qq_id")),
        string(row.get("microsoft_id")),
        string(row.get("feishu_open_id")),
        instant(row.get("created_at")),
        instant(row.get("updated_at")));
  }

  private long number(Object value) {
    return ((Number) value).longValue();
  }

  private boolean bool(Object value) {
    return Boolean.TRUE.equals(value);
  }

  private String string(Object value) {
    return value == null ? "" : value.toString();
  }

  private Instant instant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof java.time.LocalDateTime localDateTime) {
      return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
    return (Instant) value;
  }
}
