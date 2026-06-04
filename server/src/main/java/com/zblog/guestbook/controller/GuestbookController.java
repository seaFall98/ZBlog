package com.zblog.guestbook.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.guestbook.application.GuestbookService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GuestbookController {

  private final GuestbookService guestbookService;

  public GuestbookController(GuestbookService guestbookService) {
    this.guestbookService = guestbookService;
  }

  @GetMapping("/guestbook/messages")
  public ApiResponse<PageResponse<Map<String, Object>>> listPublic(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
    return ApiResponse.ok(guestbookService.listPublic(page, pageSize));
  }

  @PostMapping("/guestbook/messages")
  public ApiResponse<Map<String, Object>> submit(
      @RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
    return ApiResponse.ok(guestbookService.submit(request, servletRequest));
  }

  @GetMapping("/admin/guestbook/messages")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean pinned,
      @RequestParam(name = "start_time", required = false) String startTime,
      @RequestParam(name = "end_time", required = false) String endTime) {
    return ApiResponse.ok(guestbookService.listAdmin(page, pageSize, keyword, status, pinned, startTime, endTime));
  }

  @PutMapping("/admin/guestbook/messages/{id}/status")
  public ApiResponse<Map<String, Object>> updateStatus(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(guestbookService.updateStatus(id, request));
  }

  @PutMapping("/admin/guestbook/messages/{id}/pin")
  public ApiResponse<Map<String, Object>> updatePinned(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(guestbookService.updatePinned(id, request));
  }

  @DeleteMapping("/admin/guestbook/messages/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    guestbookService.delete(id);
    return ApiResponse.ok(null);
  }
}
