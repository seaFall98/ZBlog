package com.zblog.identity.infrastructure;

import com.zblog.identity.domain.UserAccount;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class UserAccountMapper {

  private UserAccountMapper() {}

  static Map<String, Object> toUserMap(UserAccount user, boolean includeLinkedOauths) {
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

  private static String format(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC).toString();
  }
}
