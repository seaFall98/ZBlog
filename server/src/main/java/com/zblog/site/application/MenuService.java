package com.zblog.site.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zblog.common.exception.BusinessException;
import com.zblog.site.application.port.MenuRepository;
import com.zblog.site.domain.Menu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

  private static final Set<String> V2_MENU_TYPES =
      Set.of("header_navigation", "footer_navigation");

  private final MenuRepository menuRepository;

  public MenuService(MenuRepository menuRepository) {
    this.menuRepository = menuRepository;
  }

  public List<MenuView> listMenus() {
    return tree(canonicalMenus(menuRepository.findAll()));
  }

  public List<MenuView> listAdminMenus(String type) {
    List<Menu> menus = canonicalMenus(menuRepository.findAll());
    if (type != null && !type.isBlank()) {
      String normalizedType = normalizeRequestedType(type);
      menus = menus.stream().filter(menu -> normalizedType.equals(menu.type())).toList();
    }
    return tree(menus);
  }

  public MenuView get(long id) {
    return toView(canonicalize(menuRepository.get(id)));
  }

  public FrontMenusView listFrontMenus() {
    List<Menu> menus = canonicalMenus(menuRepository.findAll());
    return new FrontMenusView(
        tree(filterByType(menus, "header_navigation")),
        tree(filterByType(menus, "footer_navigation")));
  }

  public MenuView create(Map<String, Object> request) {
    String type = normalizeRequestedType(text(request, "type"));
    Long parentId = nullableLong(request, "parent_id");
    validateParent(type, parentId);
    return toView(
        canonicalize(
            menuRepository.create(
                type,
                parentId,
                text(request, "title"),
                textOrDefault(request, "url", ""),
                textOrDefault(request, "icon", ""),
                intOrDefault(request, "sort", 0))));
  }

  public MenuView update(long id, Map<String, Object> request) {
    Menu existing = canonicalize(menuRepository.get(id));
    String type =
        request.containsKey("type")
            ? normalizeRequestedType(text(request, "type"))
            : existing.type();
    Long parentId =
        request.containsKey("parent_id") ? nullableLong(request, "parent_id") : existing.parentId();
    validateParent(type, parentId);
    return toView(
        canonicalize(
            menuRepository.update(
                id,
                type,
                parentId,
                textOrDefault(request, "title", existing.title()),
                textOrDefault(request, "url", existing.url()),
                textOrDefault(request, "icon", existing.icon()),
                intOrDefault(request, "sort", existing.sort()))));
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
      List<MenuView> children) {}

  public record FrontMenusView(List<MenuView> header, List<MenuView> footer) {}

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

  private List<Menu> canonicalMenus(List<Menu> menus) {
    return menus.stream()
        .map(this::canonicalize)
        .filter(menu -> V2_MENU_TYPES.contains(menu.type()))
        .toList();
  }

  private List<Menu> filterByType(List<Menu> menus, String type) {
    return menus.stream().filter(menu -> type.equals(menu.type())).toList();
  }

  private Menu canonicalize(Menu menu) {
    return new Menu(
        menu.id(),
        canonicalType(menu.type()),
        menu.parentId(),
        menu.title(),
        normalizeMenuUrl(menu.url()),
        menu.icon(),
        menu.sort());
  }

  private MenuView toView(Menu menu) {
    return new MenuView(
        menu.id(),
        menu.type(),
        menu.parentId(),
        menu.title(),
        menu.url(),
        menu.icon(),
        menu.sort(),
        true,
        List.of());
  }

  private String normalizeRequestedType(String type) {
    String normalized = canonicalType(type);
    if (!V2_MENU_TYPES.contains(normalized)) {
      throw new BusinessException(400, "Unsupported menu type", HttpStatus.BAD_REQUEST);
    }
    return normalized;
  }

  private String canonicalType(String type) {
    return switch (type) {
      case "navigation" -> "header_navigation";
      case "footer" -> "footer_navigation";
      default -> type;
    };
  }

  private String normalizeMenuUrl(String url) {
    return switch (url) {
      case "/album" -> "/gallery";
      case "/moment" -> "/moments";
      case "/message" -> "/guestbook";
      case "/statistics" -> "/stats";
      case "/friend" -> "/links";
      default -> url;
    };
  }

  private void validateParent(String type, Long parentId) {
    if (parentId == null) {
      return;
    }

    Menu parent = canonicalize(menuRepository.get(parentId));
    if (!type.equals(parent.type())) {
      throw new BusinessException(400, "Child menu must use the same type as its parent", HttpStatus.BAD_REQUEST);
    }

    if (parent.parentId() != null) {
      throw new BusinessException(400, "Only two menu levels are supported", HttpStatus.BAD_REQUEST);
    }
  }
}
