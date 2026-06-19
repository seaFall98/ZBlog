package com.zblog.feedback.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.feedback.application.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
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
public class FeedbackController {

  private final FeedbackService feedbackService;

  public FeedbackController(FeedbackService feedbackService) {
    this.feedbackService = feedbackService;
  }

  @PostMapping("/feedback")
  public ApiResponse<Map<String, Object>> submit(
      @RequestBody Map<String, Object> request, HttpServletRequest servletRequest, Principal principal) {
    return ApiResponse.ok(feedbackService.submit(request, servletRequest, principal));
  }

  @GetMapping("/feedback/ticket/{ticketNo}")
  public ApiResponse<Map<String, Object>> getByTicket(@PathVariable String ticketNo) {
    return ApiResponse.ok(feedbackService.getByTicket(ticketNo));
  }

  @GetMapping("/feedback/token/{accessToken}")
  public ApiResponse<Map<String, Object>> getByAccessToken(@PathVariable String accessToken) {
    return ApiResponse.ok(feedbackService.getByAccessToken(accessToken));
  }

  @GetMapping("/feedback/mine")
  public ApiResponse<PageResponse<Map<String, Object>>> mine(
      Principal principal, @RequestParam Map<String, String> params) {
    return ApiResponse.ok(feedbackService.listMine(principal.getName(), params));
  }

  @PostMapping("/feedback/{id}/messages")
  public ApiResponse<Map<String, Object>> addUserMessage(
      @PathVariable long id, @RequestBody Map<String, Object> request, Principal principal) {
    return ApiResponse.ok(feedbackService.addUserMessage(id, request, principal));
  }

  @GetMapping("/admin/feedback")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam Map<String, String> params) {
    return ApiResponse.ok(feedbackService.listAdmin(params));
  }

  @GetMapping("/admin/feedback/{id}")
  public ApiResponse<Map<String, Object>> getAdmin(@PathVariable long id) {
    return ApiResponse.ok(feedbackService.get(id));
  }

  @PutMapping("/admin/feedback/{id}")
  public ApiResponse<Map<String, Object>> updateAdmin(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(feedbackService.update(id, request));
  }

  @PostMapping("/admin/feedback/{id}/messages")
  public ApiResponse<Map<String, Object>> addAdminMessage(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(feedbackService.addAdminMessage(id, request));
  }

  @PutMapping("/admin/feedback/{id}/status")
  public ApiResponse<Map<String, Object>> updateAdminStatus(
      @PathVariable long id, @RequestBody Map<String, Object> request) {
    return ApiResponse.ok(feedbackService.updateAdminStatus(id, request));
  }

  @DeleteMapping("/admin/feedback/{id}")
  public ApiResponse<Void> deleteAdmin(@PathVariable long id) {
    feedbackService.delete(id);
    return ApiResponse.ok(null);
  }
}
