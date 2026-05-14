package com.zblog2.comment.controller;

import com.zblog2.comment.application.CommentService;
import com.zblog2.common.api.ApiResponse;
import com.zblog2.common.api.PageResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("/comments")
  public ApiResponse<PageResponse<Map<String, Object>>> listPublic(
      @RequestParam(name = "target_type") String targetType,
      @RequestParam(name = "target_key") String targetKey,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(commentService.listPublic(targetType, targetKey, page, pageSize));
  }

  @PostMapping({"/comments", "/admin/comments"})
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
    return ApiResponse.ok(commentService.create(request));
  }

  @DeleteMapping("/comments/{id}")
  public ApiResponse<Void> deletePublic(@PathVariable long id) {
    commentService.delete(id);
    return ApiResponse.ok(null);
  }

  @GetMapping("/admin/comments")
  public ApiResponse<PageResponse<Map<String, Object>>> listAdmin(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(commentService.listAdmin(page, pageSize));
  }

  @PutMapping("/admin/comments/{id}/toggle-status")
  public ApiResponse<Map<String, Object>> toggleStatus(@PathVariable long id) {
    return ApiResponse.ok(commentService.toggleStatus(id));
  }

  @DeleteMapping("/admin/comments/{id}")
  public ApiResponse<Void> deleteAdmin(@PathVariable long id) {
    commentService.delete(id);
    return ApiResponse.ok(null);
  }
}
