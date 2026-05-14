package com.zblog2.identity;

import com.zblog2.common.api.ApiResponse;
import com.zblog2.common.exception.BusinessException;
import com.zblog2.config.SecurityProperties;
import com.zblog2.security.JwtService;
import jakarta.validation.Valid;
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

    return ApiResponse.ok(
        new LoginResponse(
            jwtService.createAdminToken(request.username()),
            "Bearer",
            securityProperties.getTokenTtlMinutes() * 60));
  }
}
