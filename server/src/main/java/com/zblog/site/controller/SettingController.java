package com.zblog.site.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.site.application.SettingService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SettingController {

  private final SettingService settingService;

  public SettingController(SettingService settingService) {
    this.settingService = settingService;
  }

  @GetMapping({"/settings/{group}", "/admin/settings/{group}"})
  public ApiResponse<Map<String, String>> getGroup(@PathVariable String group) {
    return ApiResponse.ok(settingService.getGroup(group));
  }

  @GetMapping("/settings/public-profile")
  public ApiResponse<Map<String, String>> publicProfile() {
    return ApiResponse.ok(settingService.publicProfile());
  }

  @RequestMapping(
      value = "/admin/settings/{group}",
      method = {RequestMethod.PATCH, RequestMethod.PUT})
  public ApiResponse<Map<String, String>> updateGroup(
      @PathVariable String group, @RequestBody Map<String, String> request) {
    return ApiResponse.ok(settingService.updateGroup(group, request));
  }

  @PutMapping("/admin/settings/ai/mcp-secret/reset")
  public ApiResponse<Map<String, String>> resetMcpSecret() {
    return ApiResponse.ok(settingService.resetMcpSecret());
  }
}
