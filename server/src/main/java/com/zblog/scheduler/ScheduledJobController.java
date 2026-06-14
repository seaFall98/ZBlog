package com.zblog.scheduler;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/scheduled-jobs")
public class ScheduledJobController {

  private final ScheduledJobService scheduledJobService;

  public ScheduledJobController(ScheduledJobService scheduledJobService) {
    this.scheduledJobService = scheduledJobService;
  }

  @GetMapping
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    return ApiResponse.ok(scheduledJobService.list(page, pageSize));
  }

  @GetMapping("/handlers")
  public ApiResponse<List<Map<String, Object>>> handlers() {
    return ApiResponse.ok(scheduledJobService.handlers());
  }

  @PutMapping("/{id}")
  public ApiResponse<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(scheduledJobService.update(id, request));
  }

  @PutMapping("/{id}/enabled")
  public ApiResponse<Map<String, Object>> setEnabled(@PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(scheduledJobService.setEnabled(id, Boolean.TRUE.equals(request.get("enabled"))));
  }

  @PostMapping("/{id}/run")
  public ApiResponse<Map<String, Object>> runNow(@PathVariable long id) {
    return ApiResponse.ok(scheduledJobService.runNow(id));
  }

  @GetMapping("/{id}/logs")
  public ApiResponse<PageResponse<Map<String, Object>>> logs(
      @PathVariable long id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    return ApiResponse.ok(scheduledJobService.logs(id, page, pageSize));
  }
}
