package com.zblog.identity.application.port;

import com.zblog.common.api.PageResponse;
import com.zblog.identity.application.AdminUserQuery;
import com.zblog.identity.domain.UserAccount;
import java.util.Map;

public interface UserRepository {

  UserAccount findByEmail(String email);

  UserAccount findById(long id);

  boolean existsByEmail(String email);

  UserAccount create(
      String email,
      String passwordHash,
      String nickname,
      String role,
      String avatar,
      String badge,
      String website,
      boolean enabled);

  void updateLastLogin(long id);

  void updateProfile(long id, String email, String nickname, String avatar, String badge, String website);

  void updatePassword(long id, String passwordHash);

  void softDelete(long id);

  void clearOauthProvider(long id, String column);

  PageResponse<Map<String, Object>> listAdmin(AdminUserQuery query);

  void updateAdmin(
      long id,
      String email,
      String nickname,
      String avatar,
      String badge,
      String website,
      String role,
      boolean enabled);
}
