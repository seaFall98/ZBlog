package com.zblog.tools;

import com.zblog.common.api.ApiResponse;
import com.zblog.tools.application.AdminToolsService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tools")
public class AdminToolsController {

  private final AdminToolsService toolsService;

  public AdminToolsController(AdminToolsService toolsService) {
    this.toolsService = toolsService;
  }

  @PostMapping("/fetch-linkmeta")
  public ApiResponse<Map<String, Object>> fetchLinkMeta(@RequestBody Map<String, String> request) {
    return ApiResponse.ok(toolsService.fetchLinkMeta(request));
  }

  @PostMapping("/parse-video")
  public ApiResponse<Map<String, String>> parseVideo(@RequestBody Map<String, String> request) {
    return ApiResponse.ok(toolsService.parseVideo(request));
  }

  @PostMapping("/download-image")
  public ApiResponse<Map<String, Object>> downloadImage(@RequestBody Map<String, String> request) {
    return ApiResponse.ok(toolsService.downloadImage(request));
  }
}
