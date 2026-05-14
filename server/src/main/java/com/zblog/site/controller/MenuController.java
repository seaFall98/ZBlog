package com.zblog.site.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.site.application.MenuService;
import com.zblog.site.application.MenuService.MenuView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {

  private final MenuService menuService;

  public MenuController(MenuService menuService) {
    this.menuService = menuService;
  }

  @GetMapping
  public ApiResponse<List<MenuView>> listMenus() {
    return ApiResponse.ok(menuService.listMenus());
  }
}
