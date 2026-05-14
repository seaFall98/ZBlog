package com.zblog2.site.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zblog2.site.domain.Menu;
import com.zblog2.site.infrastructure.JdbcMenuRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

  private final JdbcMenuRepository menuRepository;

  public MenuService(JdbcMenuRepository menuRepository) {
    this.menuRepository = menuRepository;
  }

  public List<MenuView> listMenus() {
    return menuRepository.findAll().stream().map(MenuView::from).toList();
  }

  public record MenuView(
      long id,
      String type,
      @JsonProperty("parent_id") Long parentId,
      String title,
      String url,
      String icon,
      int sort) {
    static MenuView from(Menu menu) {
      return new MenuView(
          menu.id(),
          menu.type(),
          menu.parentId(),
          menu.title(),
          menu.url(),
          menu.icon(),
          menu.sort());
    }
  }
}
