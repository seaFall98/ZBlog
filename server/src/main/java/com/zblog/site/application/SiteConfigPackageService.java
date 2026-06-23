package com.zblog.site.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zblog.common.exception.BusinessException;
import com.zblog.site.application.MenuService.FrontMenusView;
import com.zblog.site.application.MenuService.MenuView;
import com.zblog.site.application.port.MenuRepository;
import com.zblog.site.application.port.SettingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteConfigPackageService {

  public static final String VERSION = "zblog.site-config.v1";

  private static final List<String> PORTABLE_SETTING_GROUPS =
      List.of("v2_identity", "v2_home", "v2_about", "v2_guestbook", "v2_footer", "v2_search");

  private final SettingRepository settingRepository;
  private final MenuRepository menuRepository;
  private final MenuService menuService;

  public SiteConfigPackageService(
      SettingRepository settingRepository, MenuRepository menuRepository, MenuService menuService) {
    this.settingRepository = settingRepository;
    this.menuRepository = menuRepository;
    this.menuService = menuService;
  }

  public SiteConfigPackageView exportPackage() {
    Map<String, Map<String, String>> settings = new LinkedHashMap<>();
    for (String group : PORTABLE_SETTING_GROUPS) {
      settings.put(group, new LinkedHashMap<>(settingRepository.getGroup(group)));
    }

    return new SiteConfigPackageView(VERSION, Instant.now().toString(), settings, menuService.listFrontMenus());
  }

  @Transactional
  public SiteConfigPackageView importPackage(SiteConfigPackageImportRequest request) {
    if (request == null || !VERSION.equals(request.version())) {
      throw new BusinessException(400, "Unsupported site config package version", HttpStatus.BAD_REQUEST);
    }

    Map<String, Map<String, String>> settings = request.settings() == null ? Map.of() : request.settings();
    for (String group : PORTABLE_SETTING_GROUPS) {
      settingRepository.deleteGroup(group);
      Map<String, String> values = settings.getOrDefault(group, Map.of());
      values.forEach((key, value) -> settingRepository.upsert(group, key, value == null ? "" : value));
    }

    menuRepository.deleteByTypes(List.of("header_navigation", "footer_navigation"));
    SiteConfigPackageMenus menus = request.menus() == null ? new SiteConfigPackageMenus(List.of(), List.of()) : request.menus();
    createMenuTree("header_navigation", null, menus.header());
    createMenuTree("footer_navigation", null, menus.footer());

    return exportPackage();
  }

  private void createMenuTree(String type, Long parentId, List<SiteConfigPackageMenuItem> items) {
    if (items == null) {
      return;
    }

    for (SiteConfigPackageMenuItem item : items) {
      String title = text(item.title());
      if (title.isBlank()) {
        continue;
      }

      Map<String, Object> request = new LinkedHashMap<>();
      request.put("type", type);
      request.put("parent_id", parentId);
      request.put("title", title);
      request.put("url", text(item.url()));
      request.put("icon", text(item.icon()));
      request.put("sort", item.sort() == null ? 0 : item.sort());
      MenuView created =
          menuService.create(request);
      createMenuTree(type, created.id(), item.children());
    }
  }

  private String text(String value) {
    return Objects.toString(value, "").trim();
  }

  public record SiteConfigPackageView(
      String version,
      @JsonProperty("exported_at") String exportedAt,
      Map<String, Map<String, String>> settings,
      FrontMenusView menus) {}

  public record SiteConfigPackageImportRequest(
      String version, Map<String, Map<String, String>> settings, SiteConfigPackageMenus menus) {}

  public record SiteConfigPackageMenus(
      List<SiteConfigPackageMenuItem> header, List<SiteConfigPackageMenuItem> footer) {}

  public record SiteConfigPackageMenuItem(
      String title,
      String url,
      String icon,
      Integer sort,
      List<SiteConfigPackageMenuItem> children) {}
}
