package com.zblog.ai;

import com.zblog.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai")
public class AiAdminController {

  private final AiAdminService aiAdminService;

  public AiAdminController(AiAdminService aiAdminService) {
    this.aiAdminService = aiAdminService;
  }

  @PostMapping("/test")
  public ApiResponse<Map<String, String>> test(@RequestBody Map<String, String> request) {
    return ApiResponse.ok(aiAdminService.test(request));
  }

  @PostMapping("/summary")
  public ApiResponse<Map<String, String>> summary(@RequestBody Map<String, String> request) {
    return ApiResponse.ok(aiAdminService.summary(request));
  }

  @PostMapping("/ai-summary")
  public ApiResponse<Map<String, String>> aiSummary(@RequestBody Map<String, String> request) {
    return ApiResponse.ok(aiAdminService.aiSummary(request));
  }

  @PostMapping("/title")
  public ApiResponse<Map<String, String>> title(@RequestBody Map<String, String> request) {
    return ApiResponse.ok(aiAdminService.title(request));
  }
}
