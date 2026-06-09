package com.zblog.site.application;

import com.zblog.site.application.port.SettingRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {

  private final SettingRepository settingRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  public SettingService(SettingRepository settingRepository) {
    this.settingRepository = settingRepository;
  }

  public Map<String, String> getGroup(String group) {
    return settingRepository.getGroup(group);
  }

  public Map<String, String> publicProfile() {
    Map<String, String> basic = settingRepository.getGroup("basic");
    Map<String, String> profile = new LinkedHashMap<>();
    profile.put("title", firstNonBlank(basic, "site_title", "site_name", "title"));
    profile.put("subtitle", firstNonBlank(basic, "site_subtitle", "site_description", "subtitle"));
    profile.put("aboutIntro", firstNonBlank(basic, "about_intro", "basic.author_desc", "author_desc", "about"));
    profile.put("email", firstNonBlank(basic, "contact_email", "basic.author_email", "author_email", "email"));
    profile.put("avatarUrl", firstNonBlank(basic, "avatar_url", "basic.author_avatar", "author_avatar", "avatar"));
    return profile;
  }

  @Transactional
  public Map<String, String> updateGroup(String group, Map<String, String> values) {
    values.forEach((key, value) -> settingRepository.upsert(group, key, value == null ? "" : value));
    return getGroup(group);
  }

  public Map<String, String> resetMcpSecret() {
    byte[] bytes = new byte[24];
    secureRandom.nextBytes(bytes);
    String secret = HexFormat.of().formatHex(bytes);
    settingRepository.upsert("ai", "mcp_secret", secret);
    return Map.of("secret", secret);
  }

  private String firstNonBlank(Map<String, String> values, String... keys) {
    for (String key : keys) {
      String value = values.get(key);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }
}
