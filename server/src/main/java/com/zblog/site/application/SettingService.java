package com.zblog.site.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.site.application.port.SettingRepository;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {

  private static final List<String> UPLOAD_SECRET_KEYS =
      List.of(
          "secret_id",
          "secret_key",
          "access_key",
          "access_secret",
          "client_secret",
          "upload.secret_id",
          "upload.secret_key",
          "upload.access_key",
          "upload.access_secret",
          "upload.client_secret");

  private final SettingRepository settingRepository;
  private final ObjectMapper objectMapper;
  private final SecureRandom secureRandom = new SecureRandom();

  public SettingService(SettingRepository settingRepository, ObjectMapper objectMapper) {
    this.settingRepository = settingRepository;
    this.objectMapper = objectMapper;
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

  public FrontConfigView frontConfig() {
    Map<String, String> identity = settingRepository.getGroup("v2_identity");
    Map<String, String> home = settingRepository.getGroup("v2_home");
    Map<String, String> about = settingRepository.getGroup("v2_about");
    Map<String, String> guestbook = settingRepository.getGroup("v2_guestbook");
    Map<String, String> footer = settingRepository.getGroup("v2_footer");

    return new FrontConfigView(
        new IdentityView(
            read(identity, "site_title"),
            read(identity, "owner_display_name"),
            read(identity, "email"),
            read(identity, "primary_image_url"),
            read(identity, "favicon_url"),
            read(identity, "icp_record"),
            read(identity, "police_record")),
        new HomeView(
            read(home, "hero_eyebrow"),
            read(home, "hero_title"),
            read(home, "hero_meta"),
            read(home, "hero_cta_label"),
            read(home, "hero_cta_target")),
        new AboutView(
            read(about, "intro_text"),
            parseStatusItems(read(about, "status_items")),
            parseSkillItems(read(about, "skill_items")),
            parseTimelineItems(read(about, "timeline_items")),
            read(about, "bottom_quote")),
        new GuestbookView(
            read(guestbook, "intro_text"),
            read(guestbook, "background_image"),
            clampDanmakuLimit(read(guestbook, "danmaku_public_limit"))),
        new FooterView(
            read(footer, "description"),
            read(footer, "copyright_text"),
            read(footer, "slogan"),
            parseSocialLinks(read(footer, "social_links"))));
  }

  @Transactional
  public Map<String, String> updateGroup(String group, Map<String, String> values) {
    Map<String, String> sanitized = sanitizeGroupValues(group, values);
    if ("upload".equals(group)) {
      UPLOAD_SECRET_KEYS.forEach(key -> settingRepository.deleteKey(group, key));
    }
    sanitized.forEach((key, value) -> settingRepository.upsert(group, key, value == null ? "" : value));
    return getGroup(group);
  }

  private Map<String, String> sanitizeGroupValues(String group, Map<String, String> values) {
    if (!"upload".equals(group)) {
      return values;
    }
    Map<String, String> sanitized = new LinkedHashMap<>();
    values.forEach(
        (key, value) -> {
          if (!UPLOAD_SECRET_KEYS.contains(key)) {
            sanitized.put(key, value);
          }
        });
    return sanitized;
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

  private String read(Map<String, String> values, String key) {
    String direct = values.get(key);
    if (direct != null) {
      return direct;
    }

    for (String prefix : List.of("v2_identity.", "v2_home.", "v2_about.", "v2_guestbook.", "v2_footer.")) {
      String prefixed = values.get(prefix + key);
      if (prefixed != null) {
        return prefixed;
      }
    }
    return "";
  }

  private List<StatusItemView> parseStatusItems(String raw) {
    return readJsonList(raw).stream()
        .map(
            item ->
                new StatusItemView(
                    text(item.get("icon")),
                    text(item.get("label")),
                    text(item.get("content")),
                    number(item.get("sort"))))
        .filter(item -> !item.label().isBlank() && !item.content().isBlank())
        .toList();
  }

  private List<SkillItemView> parseSkillItems(String raw) {
    return readJsonList(raw).stream()
        .map(
            item ->
                new SkillItemView(
                    text(item.get("name")), text(item.get("value")), number(item.get("sort"))))
        .filter(item -> !item.name().isBlank() && !item.value().isBlank())
        .toList();
  }

  private List<TimelineItemView> parseTimelineItems(String raw) {
    return readJsonList(raw).stream()
        .map(
            item ->
                new TimelineItemView(
                    text(item.get("year")), text(item.get("event")), number(item.get("sort"))))
        .filter(item -> !item.year().isBlank() && !item.event().isBlank())
        .toList();
  }

  private List<SocialLinkView> parseSocialLinks(String raw) {
    return readJsonList(raw).stream()
        .map(
            item ->
                new SocialLinkView(
                    text(item.get("icon")),
                    text(item.get("name")),
                    text(item.get("url")),
                    number(item.get("sort"))))
        .filter(item -> !item.name().isBlank() && !item.url().isBlank())
        .toList();
  }

  private List<Map<String, Object>> readJsonList(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }

    try {
      return objectMapper.readValue(raw, new TypeReference<List<Map<String, Object>>>() {});
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }

  private int number(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }

    if (value instanceof String text) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }

    return 0;
  }

  private int clampDanmakuLimit(String raw) {
    if (raw == null || raw.isBlank()) return 200;
    try {
      int value = Integer.parseInt(raw.trim());
      return Math.min(500, Math.max(50, value));
    } catch (NumberFormatException ignored) {
      return 200;
    }
  }

  public record FrontConfigView(
      IdentityView identity,
      HomeView home,
      AboutView about,
      GuestbookView guestbook,
      FooterView footer) {}

  public record IdentityView(
      String siteTitle,
      String ownerDisplayName,
      String email,
      String primaryImageUrl,
      String faviconUrl,
      String icpRecord,
      String policeRecord) {}

  public record HomeView(
      String heroEyebrow,
      String heroTitle,
      String heroMeta,
      String heroCtaLabel,
      String heroCtaTarget) {}

  public record AboutView(
      String introText,
      List<StatusItemView> statusItems,
      List<SkillItemView> skillItems,
      List<TimelineItemView> timelineItems,
      String bottomQuote) {}

  public record StatusItemView(String icon, String label, String content, int sort) {}

  public record SkillItemView(String name, String value, int sort) {}

  public record TimelineItemView(String year, String event, int sort) {}

  public record GuestbookView(String introText, String backgroundImage, int danmakuPublicLimit) {}

  public record FooterView(
      String description,
      String copyrightText,
      String slogan,
      List<SocialLinkView> socialLinks) {}

  public record SocialLinkView(String icon, String name, String url, int sort) {}
}
