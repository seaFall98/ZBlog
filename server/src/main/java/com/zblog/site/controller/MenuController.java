package com.zblog.site.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.site.application.MenuService;
import com.zblog.site.application.MenuService.FrontMenusView;
import com.zblog.site.application.MenuService.MenuView;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MenuController {

  private final MenuService menuService;

  public MenuController(MenuService menuService) {
    this.menuService = menuService;
  }

  @GetMapping("/menus")
  public ApiResponse<List<MenuView>> listMenus() {
    return ApiResponse.ok(menuService.listMenus());
  }

  @GetMapping("/front/menus")
  public ApiResponse<FrontMenusView> listFrontMenus() {
    return ApiResponse.ok(menuService.listFrontMenus());
  }

  @GetMapping("/admin/menus")
  public ApiResponse<List<MenuView>> listAdminMenus(@RequestParam(required = false) String type) {
    return ApiResponse.ok(menuService.listAdminMenus(type));
  }

  @PostMapping("/admin/menus")
  public ApiResponse<MenuView> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(menuService.create(request));
  }

  @GetMapping("/admin/menus/{id}")
  public ApiResponse<MenuView> get(@PathVariable long id) {
    return ApiResponse.ok(menuService.get(id));
  }

  @PutMapping("/admin/menus/{id}")
  public ApiResponse<MenuView> update(@PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(menuService.update(id, request));
  }

  @DeleteMapping("/admin/menus/{id}")
  public ApiResponse<Void> delete(@PathVariable long id, @RequestBody(required = false) Map<String, Object> request) {
    String childrenAction = request == null ? "upgrade" : String.valueOf(request.getOrDefault("children_action", "upgrade"));
    menuService.delete(id, childrenAction);
    return ApiResponse.ok(null);
  }
}
