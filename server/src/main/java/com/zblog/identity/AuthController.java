package com.zblog.identity;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.config.SecurityProperties;
import com.zblog.security.JwtService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final JwtService jwtService;
  private final SecurityProperties securityProperties;

  public AuthController(JwtService jwtService, SecurityProperties securityProperties) {
    this.jwtService = jwtService;
    this.securityProperties = securityProperties;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    SecurityProperties.BootstrapAdmin bootstrapAdmin = securityProperties.getBootstrapAdmin();
    if (!bootstrapAdmin.getUsername().equals(request.username())
        || !bootstrapAdmin.getPassword().equals(request.password())) {
      throw new BusinessException(
          401, "Invalid username or password", HttpStatus.UNAUTHORIZED);
    }

    return tokenResponse(request.username());
  }

  @PostMapping("/refresh")
  public ApiResponse<LoginResponse> refresh(Principal principal) {
    return tokenResponse(principal.getName());
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.ok(null);
  }

  private ApiResponse<LoginResponse> tokenResponse(String username) {
    return ApiResponse.ok(
        new LoginResponse(
            jwtService.createAdminToken(username),
            "Bearer",
            securityProperties.getTokenTtlMinutes() * 60));
  }
}
