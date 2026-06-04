package com.zblog.identity;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.identity.application.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(userService.login(request.username(), request.password()));
  }

  @PostMapping("/register")
  public ApiResponse<LoginResponse> register(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(userService.register(request));
  }

  @PostMapping("/forgot-password")
  public ApiResponse<Map<String, Object>> forgotPassword(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(userService.forgotPassword(request));
  }

  @PostMapping("/reset-password")
  public ApiResponse<Void> resetPassword(@RequestBody Map<String, Object> request) {
    userService.resetPassword(request);
    return ApiResponse.ok(null);
  }

  @GetMapping("/{provider}")
  public ApiResponse<Void> oauthBegin(@PathVariable String provider) {
    throw new BusinessException(501, "OAuth " + provider + " unsupported without provider credentials", HttpStatus.NOT_IMPLEMENTED);
  }

  @PostMapping("/refresh")
  public ApiResponse<LoginResponse> refresh(Principal principal) {
    return ApiResponse.ok(userService.refresh(principal.getName()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.ok(null);
  }
}
