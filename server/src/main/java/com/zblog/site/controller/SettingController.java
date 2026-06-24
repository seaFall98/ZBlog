package com.zblog.site.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.site.application.SettingService;
import com.zblog.site.application.SettingService.FrontConfigView;
import com.zblog.site.application.SiteConfigPackageService;
import com.zblog.site.application.SiteConfigPackageService.SiteConfigPackageImportRequest;
import com.zblog.site.application.SiteConfigPackageService.SiteConfigPackageView;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SettingController {

  private final SettingService settingService;
  private final SiteConfigPackageService siteConfigPackageService;

  public SettingController(SettingService settingService, SiteConfigPackageService siteConfigPackageService) {
    this.settingService = settingService;
    this.siteConfigPackageService = siteConfigPackageService;
  }

  @GetMapping({"/settings/{group}", "/admin/settings/{group}"})
  public ApiResponse<Map<String, String>> getGroup(@PathVariable String group) {
    return ApiResponse.ok(settingService.getGroup(group));
  }

  @GetMapping("/settings/public-profile")
  public ApiResponse<Map<String, String>> publicProfile() {
    return ApiResponse.ok(settingService.publicProfile());
  }

  @GetMapping("/front/config")
  public ApiResponse<FrontConfigView> frontConfig() {
    return ApiResponse.ok(settingService.frontConfig());
  }

  @GetMapping("/admin/site-config/package")
  public ApiResponse<SiteConfigPackageView> exportSiteConfigPackage() {
    return ApiResponse.ok(siteConfigPackageService.exportPackage());
  }

  @PutMapping("/admin/site-config/package")
  public ApiResponse<SiteConfigPackageView> importSiteConfigPackage(
      @RequestBody SiteConfigPackageImportRequest request) {
    return ApiResponse.ok(siteConfigPackageService.importPackage(request));
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
