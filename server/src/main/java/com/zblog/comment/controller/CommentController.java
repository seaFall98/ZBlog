package com.zblog.comment.controller;

import com.zblog.comment.application.CommentService;
import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status,
      @RequestParam(name = "is_deleted", required = false) Boolean deleted,
      @RequestParam(name = "is_sub", required = false) Boolean sub,
      @RequestParam(name = "start_time", required = false) String startTime,
      @RequestParam(name = "end_time", required = false) String endTime) {
    return ApiResponse.ok(commentService.listAdmin(page, pageSize, keyword, status, deleted, sub, startTime, endTime));
  }

  @PutMapping("/admin/comments/{id}/toggle-status")
  public ApiResponse<Map<String, Object>> toggleStatus(@PathVariable long id) {
    return ApiResponse.ok(commentService.toggleStatus(id));
  }

  @PostMapping(value = "/admin/comments/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<Map<String, Object>> importComments(
      @RequestParam(name = "source_type", defaultValue = "artalk") String sourceType,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(commentService.importComments(sourceType, file));
  }

  @DeleteMapping("/admin/comments/{id}")
  public ApiResponse<Void> deleteAdmin(@PathVariable long id) {
    commentService.delete(id);
    return ApiResponse.ok(null);
  }
}
