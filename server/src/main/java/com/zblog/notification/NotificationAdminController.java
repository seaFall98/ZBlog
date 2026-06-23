package com.zblog.notification;

import com.zblog.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NotificationAdminController {

  private final NotificationService notificationService;

  public NotificationAdminController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/admin/notifications")
  public ApiResponse<Map<String, Object>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Boolean read,
      @RequestParam(required = false) Boolean processed,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(notificationService.listAdmin(page, pageSize, type, read, processed, keyword));
  }

  @PutMapping("/admin/notifications/{id}/read")
  public ApiResponse<Map<String, Object>> read(@PathVariable long id) {
    return ApiResponse.ok(notificationService.markRead(id));
  }

  @PutMapping("/admin/notifications/read-all")
  public ApiResponse<Map<String, Object>> readAll() {
    return ApiResponse.ok(notificationService.markAllRead());
  }

  @PutMapping("/admin/notifications/{id}/processed")
  public ApiResponse<Map<String, Object>> processed(@PathVariable long id) {
    return ApiResponse.ok(notificationService.markProcessed(id, true));
  }

  @PutMapping("/admin/notifications/{id}/unprocessed")
  public ApiResponse<Map<String, Object>> unprocessed(@PathVariable long id) {
    return ApiResponse.ok(notificationService.markProcessed(id, false));
  }
}
