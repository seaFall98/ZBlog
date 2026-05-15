package com.zblog.identity;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

  private final UserService userService;

  public AdminUserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public ApiResponse<PageResponse<Map<String, Object>>> list(@RequestParam Map<String, String> params) {
    return ApiResponse.ok(userService.listAdmin(params));
  }

  @GetMapping("/{id}")
  public ApiResponse<Map<String, Object>> get(@PathVariable long id) {
    return ApiResponse.ok(userService.getAdmin(id));
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(userService.createAdmin(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(userService.updateAdmin(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    userService.deleteAdmin(id);
    return ApiResponse.ok(null);
  }

  @PutMapping("/{id}/password")
  public ApiResponse<Void> resetPassword(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    userService.resetPasswordAdmin(id, request);
    return ApiResponse.ok(null);
  }
}
