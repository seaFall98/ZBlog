package com.zblog.ai;

import com.zblog.ai.application.AiAdminService;
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
  public ApiResponse<Map<String, Object>> test(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(aiAdminService.test(request));
  }

  @PostMapping("/summary")
  public ApiResponse<Map<String, Object>> summary(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(aiAdminService.summary(request));
  }

  @PostMapping("/ai-summary")
  public ApiResponse<Map<String, Object>> aiSummary(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(aiAdminService.aiSummary(request));
  }

  @PostMapping("/title")
  public ApiResponse<Map<String, Object>> title(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(aiAdminService.title(request));
  }
}
