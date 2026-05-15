package com.zblog.media.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.media.application.FileService;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class FileController {

  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @PostMapping("/admin/files")
  public ApiResponse<Map<String, Object>> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam(name = "type", defaultValue = "image") String type)
      throws IOException {
    return ApiResponse.ok(fileService.upload(file, type));
  }

  @PostMapping("/upload")
  public ApiResponse<Map<String, Object>> uploadFeedbackAttachment(@RequestPart("file") MultipartFile file)
      throws IOException {
    return ApiResponse.ok(fileService.upload(file, "反馈投诉"));
  }

  @GetMapping("/admin/files")
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(fileService.list(page, pageSize));
  }

  @DeleteMapping("/admin/files/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    fileService.delete(id);
    return ApiResponse.ok(null);
  }
}
