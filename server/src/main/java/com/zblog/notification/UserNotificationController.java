package com.zblog.notification;

import com.zblog.common.api.ApiResponse;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class UserNotificationController {

  private final NotificationService notificationService;

  public UserNotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> list(
      Principal principal,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "10") int pageSize,
      @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly) {
    return ApiResponse.ok(notificationService.listForUser(principal.getName(), page, pageSize, unreadOnly));
  }

  @GetMapping("/unread-count")
  public ApiResponse<Map<String, Object>> unreadCount(Principal principal) {
    return ApiResponse.ok(notificationService.unreadCountForUser(principal.getName()));
  }

  @PutMapping("/{id}/read")
  public ApiResponse<Map<String, Object>> read(Principal principal, @PathVariable long id) {
    return ApiResponse.ok(notificationService.markReadForUser(principal.getName(), id));
  }

  @PutMapping("/read-all")
  public ApiResponse<Map<String, Object>> readAll(Principal principal) {
    return ApiResponse.ok(notificationService.markAllReadForUser(principal.getName()));
  }
}
