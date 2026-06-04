package com.zblog.site.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zblog.site.application.port.MenuRepository;
import com.zblog.site.domain.Menu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

  private final MenuRepository menuRepository;

  public MenuService(MenuRepository menuRepository) {
    this.menuRepository = menuRepository;
  }

  public List<MenuView> listMenus() {
    return tree(menuRepository.findAll());
  }

  public List<MenuView> listAdminMenus(String type) {
    List<Menu> menus = menuRepository.findAll();
    if (type != null && !type.isBlank()) {
      menus = menus.stream().filter(menu -> type.equals(menu.type())).toList();
    }
    return tree(menus);
  }

  public MenuView get(long id) {
    return MenuView.from(menuRepository.get(id));
  }

  public MenuView create(Map<String, Object> request) {
    return MenuView.from(
        menuRepository.create(
            text(request, "type"),
            nullableLong(request, "parent_id"),
            text(request, "title"),
            textOrDefault(request, "url", ""),
            textOrDefault(request, "icon", ""),
            intOrDefault(request, "sort", 0)));
  }

  public MenuView update(long id, Map<String, Object> request) {
    Menu existing = menuRepository.get(id);
    return MenuView.from(
        menuRepository.update(
            id,
            textOrDefault(request, "type", existing.type()),
            request.containsKey("parent_id") ? nullableLong(request, "parent_id") : existing.parentId(),
            textOrDefault(request, "title", existing.title()),
            textOrDefault(request, "url", existing.url()),
            textOrDefault(request, "icon", existing.icon()),
            intOrDefault(request, "sort", existing.sort())));
  }

  public void delete(long id, String childrenAction) {
    menuRepository.delete(id, "delete".equals(childrenAction));
  }

  private List<MenuView> tree(List<Menu> menus) {
    Map<Long, MutableMenuView> indexed = new LinkedHashMap<>();
    for (Menu menu : menus) {
      indexed.put(menu.id(), MutableMenuView.from(menu));
    }

    List<MutableMenuView> roots = new ArrayList<>();
    for (MutableMenuView menu : indexed.values()) {
      if (menu.parentId == null) {
        roots.add(menu);
        continue;
      }
      MutableMenuView parent = indexed.get(menu.parentId);
      if (parent == null) {
        roots.add(menu);
      } else {
        parent.children.add(menu);
      }
    }

    return roots.stream().map(MutableMenuView::immutable).toList();
  }

  public record MenuView(
      long id,
      String type,
      @JsonProperty("parent_id") Long parentId,
      String title,
      String url,
      String icon,
      int sort,
      @JsonProperty("is_enabled") boolean enabled,
      List<MenuView> children) {
    static MenuView from(Menu menu) {
      return new MenuView(
          menu.id(), menu.type(), menu.parentId(), menu.title(), menu.url(), menu.icon(), menu.sort(), true, List.of());
    }
  }

  private static final class MutableMenuView {
    private final long id;
    private final String type;
    private final Long parentId;
    private final String title;
    private final String url;
    private final String icon;
    private final int sort;
    private final List<MutableMenuView> children = new ArrayList<>();

    private MutableMenuView(Menu menu) {
      this.id = menu.id();
      this.type = menu.type();
      this.parentId = menu.parentId();
      this.title = menu.title();
      this.url = menu.url();
      this.icon = menu.icon();
      this.sort = menu.sort();
    }

    private static MutableMenuView from(Menu menu) {
      return new MutableMenuView(menu);
    }

    private MenuView immutable() {
      return new MenuView(
          id, type, parentId, title, url, icon, sort, true, children.stream().map(MutableMenuView::immutable).toList());
    }
  }

  private String text(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value == null ? "" : value.toString().trim();
  }

  private String textOrDefault(Map<String, Object> request, String key, String defaultValue) {
    String value = text(request, key);
    return value.isBlank() ? defaultValue : value;
  }

  private Long nullableLong(Map<String, Object> request, String key) {
    Object value = request.get(key);
    return value instanceof Number number ? number.longValue() : null;
  }

  private int intOrDefault(Map<String, Object> request, String key, int defaultValue) {
    Object value = request.get(key);
    return value instanceof Number number ? number.intValue() : defaultValue;
  }
}
