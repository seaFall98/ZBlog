package com.zblog2.media.controller;

import com.zblog2.common.api.ApiResponse;
import com.zblog2.common.api.PageResponse;
import com.zblog2.media.application.FileService;
import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/files")
public class FileController {

  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam(name = "type", defaultValue = "image") String type)
      throws IOException {
    return ApiResponse.ok(fileService.upload(file, type));
  }

  @GetMapping
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize) {
    return ApiResponse.ok(fileService.list(page, pageSize));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    fileService.delete(id);
    return ApiResponse.ok(null);
  }
}
