package com.zblog.identity;

import com.zblog.common.api.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class CurrentUserController {

  @GetMapping("/profile")
  public ApiResponse<Map<String, Object>> profile(Authentication authentication) {
    String username = authentication.getName();
    return ApiResponse.ok(
        Map.ofEntries(
            Map.entry("id", 1L),
            Map.entry("email", username),
            Map.entry("nickname", username),
            Map.entry("avatar", ""),
            Map.entry("role", "super_admin"),
            Map.entry("is_enabled", true),
            Map.entry("last_login", OffsetDateTime.now().toString()),
            Map.entry("has_password", true),
            Map.entry("github_id", ""),
            Map.entry("google_id", ""),
            Map.entry("qq_id", ""),
            Map.entry("microsoft_id", ""),
            Map.entry("feishu_open_id", "")));
  }
}
