package com.zblog.identity;

import com.zblog.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
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

  @PostMapping("/refresh")
  public ApiResponse<LoginResponse> refresh(Principal principal) {
    return ApiResponse.ok(userService.refresh(principal.getName()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.ok(null);
  }
}
