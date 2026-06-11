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
    Map<String, String> blog = settingRepository.getGroup("blog");
    Map<String, String> profile = new LinkedHashMap<>();
    profile.put("title", firstNonBlank(blog, basic, "title", "site_title", "site_name"));
    profile.put("subtitle", firstNonBlank(blog, basic, "subtitle", "site_subtitle", "site_description"));
    profile.put("aboutIntro", firstNonBlank(blog, basic, "about_describe", "about_intro", "author_desc", "about"));
    profile.put("email", firstNonBlank(basic, blog, "contact_email", "author_email", "email"));
    profile.put("avatarUrl", firstNonBlank(basic, blog, "avatar_url", "author_avatar", "avatar"));
    profile.put("faviconUrl", firstNonBlank(blog, basic, "favicon", "favicon_url"));
    profile.put("established", firstNonBlank(blog, basic, "established"));
    profile.put("heroEyebrow", firstNonBlank(blog, basic, "hero_eyebrow"));
    profile.put("heroTitle", firstNonBlank(blog, basic, "hero_title", "title"));
    profile.put("heroSlogan", firstNonBlank(blog, basic, "hero_slogan", "slogan"));
    profile.put("footerDescription", firstNonBlank(blog, basic, "footer_description", "description", "subtitle", "site_description"));
    profile.put("footerCopyright", firstNonBlank(blog, basic, "footer_copyright"));
    profile.put("footerSlogan", firstNonBlank(blog, basic, "footer_slogan", "slogan"));
    profile.put("backgroundImage", firstNonBlank(blog, basic, "background_image"));
    profile.put("barrageBackgroundImage", firstNonBlank(blog, basic, "barrage_background_image"));
    profile.put("messageContent", firstNonBlank(blog, basic, "message_content"));
    profile.put("aboutDescribe", firstNonBlank(blog, basic, "about_describe"));
    profile.put("aboutDescribeTips", firstNonBlank(blog, basic, "about_describe_tips"));
    profile.put("aboutExhibition", firstNonBlank(blog, basic, "about_exhibition"));
    profile.put("aboutProfile", firstNonBlank(blog, basic, "about_profile"));
    profile.put("aboutPersonality", firstNonBlank(blog, basic, "about_personality"));
    profile.put("aboutMottoMain", firstNonBlank(blog, basic, "about_motto_main"));
    profile.put("aboutMottoSub", firstNonBlank(blog, basic, "about_motto_sub"));
    profile.put("aboutStory", firstNonBlank(blog, basic, "about_story"));
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

  private String firstNonBlank(Map<String, String> primary, Map<String, String> secondary, String... keys) {
    String value = firstNonBlank(primary, keys);
    return value.isBlank() ? firstNonBlank(secondary, keys) : value;
  }

  private String firstNonBlank(Map<String, String> values, String... keys) {
    for (String key : keys) {
      String value = values.get(key);
      if (value != null && !value.isBlank()) {
        return value;
      }
      value = values.get("blog." + key);
      if (value != null && !value.isBlank()) {
        return value;
      }
      value = values.get("basic." + key);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }
}
