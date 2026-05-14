package com.zblog.admin;

import com.zblog.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminPingController {

  @GetMapping("/ping")
  public ApiResponse<Map<String, String>> ping() {
    return ApiResponse.ok(Map.of("message", "pong"));
  }
}
