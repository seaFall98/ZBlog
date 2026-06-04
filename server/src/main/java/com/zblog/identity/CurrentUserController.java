package com.zblog.identity;

import com.zblog.common.api.ApiResponse;
import com.zblog.identity.application.UserService;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class CurrentUserController {

  private final UserService userService;

  public CurrentUserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/profile")
  public ApiResponse<Map<String, Object>> profile(Principal principal) {
    return ApiResponse.ok(userService.profile(principal.getName()));
  }

  @PatchMapping("/profile")
  public ApiResponse<Map<String, Object>> patchProfile(
      Principal principal, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(userService.updateProfile(principal.getName(), request));
  }

  @PutMapping("/profile")
  public ApiResponse<Map<String, Object>> putProfile(
      Principal principal, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(userService.updateProfile(principal.getName(), request));
  }

  @PutMapping("/password")
  public ApiResponse<Void> changePassword(
      Principal principal, @RequestBody Map<String, Object> request) {
    userService.changePassword(principal.getName(), request);
    return ApiResponse.ok(null);
  }

  @PostMapping("/password")
  public ApiResponse<Void> setPassword(
      Principal principal, @RequestBody Map<String, Object> request) {
    userService.setPassword(principal.getName(), request);
    return ApiResponse.ok(null);
  }

  @DeleteMapping("/deactivate")
  public ApiResponse<Void> deactivate(Principal principal, @RequestBody Map<String, Object> request) {
    userService.deactivate(principal.getName(), request);
    return ApiResponse.ok(null);
  }

  @DeleteMapping("/oauth/{provider}")
  public ApiResponse<Void> unbindOAuth(Principal principal, @PathVariable String provider) {
    userService.unbindOAuth(principal.getName(), provider);
    return ApiResponse.ok(null);
  }
}
