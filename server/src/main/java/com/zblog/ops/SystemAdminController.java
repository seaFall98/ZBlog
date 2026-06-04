package com.zblog.ops;

import com.zblog.common.api.ApiResponse;
import com.zblog.ops.application.SystemInfoService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system")
public class SystemAdminController {

  private final SystemInfoService systemInfoService;

  public SystemAdminController(SystemInfoService systemInfoService) {
    this.systemInfoService = systemInfoService;
  }

  @GetMapping("/static")
  public ApiResponse<Map<String, Object>> staticInfo() {
    return ApiResponse.ok(systemInfoService.staticInfo());
  }

  @GetMapping("/dynamic")
  public ApiResponse<Map<String, Object>> dynamicInfo() {
    return ApiResponse.ok(systemInfoService.dynamicInfo());
  }

  @PostMapping("/check-update")
  public ApiResponse<Map<String, Object>> checkUpdate() {
    return ApiResponse.ok(systemInfoService.checkUpdate());
  }
}
