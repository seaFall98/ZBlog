package com.zblog.identity.application;

import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.config.SecurityProperties;
import com.zblog.identity.LoginResponse;
import com.zblog.identity.application.port.PasswordResetMailer;
import com.zblog.identity.application.port.PasswordResetTokenRepository;
import com.zblog.identity.application.port.UserRepository;
import com.zblog.identity.domain.UserAccount;
import com.zblog.security.JwtService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

  // 身份应用服务汇聚用户自助、后台管理、JWT、密码和 OAuth 登录边界。
  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordResetMailer passwordResetMailer;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SecurityProperties securityProperties;
  private final LoginAttemptLimiter loginAttemptLimiter;
  private final SecureRandom secureRandom = new SecureRandom();
  private static final Set<String> RESERVED_BADGES =
      Set.of("站长", "博主", "管理员", "admin", "root", "super_admin");

  public UserService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      PasswordResetMailer passwordResetMailer,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      SecurityProperties securityProperties,
      LoginAttemptLimiter loginAttemptLimiter) {
    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.passwordResetMailer = passwordResetMailer;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.securityProperties = securityProperties;
    this.loginAttemptLimiter = loginAttemptLimiter;
  }

  public LoginResponse login(String username, String password) {
    return login(username, password, "unknown");
  }

  public LoginResponse login(String username, String password, String remoteAddress) {
    String email = username == null ? "" : username.trim().toLowerCase();
    loginAttemptLimiter.assertNotLocked(email, remoteAddress);
    try {
      UserAccount user = userRepository.findByEmail(email);
      if (!user.enabled() || user.deletedAt() != null) {
        throw new BusinessException(401, "Invalid username or password", HttpStatus.UNAUTHORIZED);
      }
      if (!StringUtils.hasText(user.passwordHash())
          || !passwordEncoder.matches(password, user.passwordHash())) {
        throw new BusinessException(401, "Invalid username or password", HttpStatus.UNAUTHORIZED);
      }
      loginAttemptLimiter.clear(email, remoteAddress);
      userRepository.updateLastLogin(user.id());
      return tokenResponse(userRepository.findById(user.id()));
    } catch (BusinessException exception) {
      if (exception.status() == HttpStatus.UNAUTHORIZED) {
        loginAttemptLimiter.recordFailure(email, remoteAddress);
      }
      throw new BusinessException(401, "Invalid username or password", HttpStatus.UNAUTHORIZED);
    }
  }

  public LoginResponse register(Map<String, Object> request) {
    String email = requiredString(request, "email");
    String nickname = requiredString(request, "nickname");
    String password = requiredString(request, "password");
    String website = stringValue(request.get("website"));
    if (userRepository.existsByEmail(email)) {
      throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
    }
    UserAccount user = createUser(email, password, nickname, "user", "", null, website, true);
    return tokenResponse(user);
  }

  public LoginResponse refresh(String email) {
    return tokenResponse(findActiveByEmail(email));
  }

  public Map<String, Object> forgotPassword(Map<String, Object> request) {
    String email = requiredString(request, "email").toLowerCase();
    UserAccount user;
    try {
      user = findActiveByEmail(email);
    } catch (BusinessException exception) {
      return Map.of("sent", true);
    }
    byte[] bytes = new byte[24];
    secureRandom.nextBytes(bytes);
    String token = HexFormat.of().formatHex(bytes);
    // 重置 token 保持短时一次性语义，邮件投递通过 outbox 与用户流程解耦。
    passwordResetTokenRepository.create(user.email(), token, LocalDateTime.now().plusMinutes(30));
    passwordResetMailer.sendResetToken(user.email(), token);
    return Map.of("sent", true);
  }

  public void resetPassword(Map<String, Object> request) {
    String email = requiredString(request, "email").toLowerCase();
    String token = requiredString(request, "code");
    String password = requiredString(request, "password");
    long tokenId =
        passwordResetTokenRepository
            .findNewestUsableTokenId(email, token)
            .orElseThrow(() -> new BusinessException(400, "Invalid or expired reset token", HttpStatus.BAD_REQUEST));
    UserAccount user = findActiveByEmail(email);
    updatePassword(user.id(), password);
    passwordResetTokenRepository.markUsed(tokenId);
  }

  public Map<String, Object> profile(String email) {
    return toUserMap(findActiveByEmail(email), true);
  }

  public Map<String, Object> updateProfile(String email, Map<String, Object> request) {
    UserAccount user = findActiveByEmail(email);
    String nickname = optionalString(request, "nickname", user.nickname());
    String newEmail = optionalString(request, "email", user.email()).toLowerCase();
    String avatar = optionalString(request, "avatar", user.avatar());
    String badge = nullableOrExisting(request, "badge", user.badge());
    String website = nullableOrExisting(request, "website", user.website());
    String bio = nullableOrExisting(request, "bio", user.bio());
    validateBadge(badge);
    if (!newEmail.equals(user.email())) {
      String currentPassword = requiredString(request, "current_password");
      if (!StringUtils.hasText(user.passwordHash())
          || !passwordEncoder.matches(currentPassword, user.passwordHash())) {
        throw new BusinessException(400, "当前密码错误", HttpStatus.BAD_REQUEST);
      }
      if (userRepository.existsByEmail(newEmail)) {
        throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
      }
    }
    userRepository.updateProfile(user.id(), newEmail, nickname, avatar, badge, website, bio);
    return toUserMap(userRepository.findById(user.id()), true);
  }

  public void changePassword(String email, Map<String, Object> request) {
    UserAccount user = findActiveByEmail(email);
    String oldPassword = requiredString(request, "old_password");
    String newPassword = requiredString(request, "new_password");
    if (!StringUtils.hasText(user.passwordHash())
        || !passwordEncoder.matches(oldPassword, user.passwordHash())) {
      throw new BusinessException(400, "旧密码错误", HttpStatus.BAD_REQUEST);
    }
    updatePassword(user.id(), newPassword);
  }

  public void setPassword(String email, Map<String, Object> request) {
    UserAccount user = findActiveByEmail(email);
    if (StringUtils.hasText(user.passwordHash())) {
      throw new BusinessException(400, "Password already set", HttpStatus.BAD_REQUEST);
    }
    updatePassword(user.id(), requiredString(request, "password"));
  }

  public void deactivate(String email, Map<String, Object> request) {
    UserAccount user = findActiveByEmail(email);
    String password = requiredString(request, "password");
    if (!StringUtils.hasText(user.passwordHash())
        || !passwordEncoder.matches(password, user.passwordHash())) {
      throw new BusinessException(400, "密码错误", HttpStatus.BAD_REQUEST);
    }
    userRepository.softDelete(user.id());
  }

  public void unbindOAuth(String email, String provider) {
    UserAccount user = findActiveByEmail(email);
    String column = oauthColumn(provider);
    int loginMethods = loginMethodCount(user);
    if (loginMethods <= 1) {
      // OAuth 解绑必须保留至少一种登录方式，避免用户把自己锁在账号外。
      throw new BusinessException(400, "至少保留一种登录方式", HttpStatus.BAD_REQUEST);
    }
    userRepository.clearOauthProvider(user.id(), column);
  }

  public PageResponse<Map<String, Object>> listAdmin(Map<String, String> params) {
    return userRepository.listAdmin(new AdminUserQuery(params));
  }

  public Map<String, Object> getAdmin(long id) {
    return toUserMap(userRepository.findById(id), false);
  }

  public Map<String, Object> createAdmin(Map<String, Object> request) {
    String email = requiredString(request, "email");
    if (userRepository.existsByEmail(email)) {
      throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
    }
    UserAccount user =
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
    UserAccount user = userRepository.findById(id);
    String email = optionalString(request, "email", user.email());
    if (!email.equals(user.email()) && userRepository.existsByEmail(email)) {
      throw new BusinessException(409, "Email already exists", HttpStatus.CONFLICT);
    }
    userRepository.updateAdmin(
        id,
        email,
        optionalString(request, "nickname", user.nickname()),
        optionalString(request, "avatar", user.avatar()),
        nullableOrExisting(request, "badge", user.badge()),
        nullableOrExisting(request, "website", user.website()),
        nullableOrExisting(request, "bio", user.bio()),
        optionalString(request, "role", user.role()),
        optionalBoolean(request, "is_enabled", user.enabled()));
    if (StringUtils.hasText(stringValue(request.get("password")))) {
      updatePassword(id, stringValue(request.get("password")));
    }
    return toUserMap(userRepository.findById(id), false);
  }

  public void deleteAdmin(long id) {
    UserAccount user = userRepository.findById(id);
    if ("super_admin".equals(user.role())) {
      // super_admin 是后台兜底治理账号，只允许停用普通管理员。
      throw new BusinessException(400, "Cannot delete super admin", HttpStatus.BAD_REQUEST);
    }
    userRepository.softDelete(id);
  }

  public void resetPasswordAdmin(long id, Map<String, Object> request) {
    userRepository.findById(id);
    updatePassword(id, requiredString(request, "new_password"));
  }

  private UserAccount createUser(
      String email,
      String password,
      String nickname,
      String role,
      String avatar,
      String badge,
      String website,
      boolean enabled) {
    return userRepository.create(email, passwordEncoder.encode(password), nickname, role, avatar, badge, website, enabled);
  }

  private LoginResponse tokenResponse(UserAccount user) {
    return new LoginResponse(
        jwtService.createUserToken(user.email(), user.role()),
        "Bearer",
        securityProperties.getTokenTtlMinutes() * 60,
        toUserMap(user, true));
  }

  private void updatePassword(long id, String password) {
    userRepository.updatePassword(id, passwordEncoder.encode(password));
  }

  private UserAccount findActiveByEmail(String email) {
    UserAccount user = userRepository.findByEmail(email);
    if (!user.enabled() || user.deletedAt() != null) {
      throw new BusinessException(401, "User is disabled", HttpStatus.UNAUTHORIZED);
    }
    return user;
  }

  private Map<String, Object> toUserMap(UserAccount user, boolean includeLinkedOauths) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", user.id());
    map.put("email", user.email());
    map.put("email_hash", "");
    map.put("is_virtual_email", false);
    map.put("nickname", user.nickname());
    map.put("avatar", user.avatar());
    map.put("badge", user.badge());
    map.put("website", user.website());
    map.put("bio", user.bio());
    map.put("role", user.role());
    map.put("is_enabled", user.enabled());
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

  private void validateBadge(String badge) {
    if (!StringUtils.hasText(badge)) {
      return;
    }
    String normalized = badge.trim().toLowerCase();
    if (RESERVED_BADGES.contains(normalized) || RESERVED_BADGES.contains(badge.trim())) {
      throw new BusinessException(400, "Badge is reserved", HttpStatus.BAD_REQUEST);
    }
  }

  private int loginMethodCount(UserAccount user) {
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

  private String format(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC).toString();
  }
}
