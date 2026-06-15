package com.zblog.media.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import com.zblog.common.exception.BusinessException;
import com.zblog.media.application.FileService;
import java.io.IOException;
import java.util.Map;
import java.security.Principal;
import org.springframework.http.HttpStatus;
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
  public ApiResponse<Map<String, Object>> uploadPublic(
      @RequestPart("file") MultipartFile file,
      @RequestParam(name = "type", defaultValue = "评论贴图") String type,
      Principal principal)
      throws IOException {
    if ("用户头像".equals(type) && principal == null) {
      throw new BusinessException(401, "Unauthorized", HttpStatus.UNAUTHORIZED);
    }
    return ApiResponse.ok(fileService.upload(file, publicUploadType(type)));
  }

  private String publicUploadType(String type) {
    return switch (type) {
      case "用户头像", "评论贴图", "反馈投诉" -> type;
      default -> throw new BusinessException(40001, "不支持的上传类型", HttpStatus.BAD_REQUEST);
    };
  }

  @GetMapping("/admin/files")
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "50") int pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(name = "file_type", required = false) String fileType,
      @RequestParam(required = false) Integer status,
      @RequestParam(name = "upload_type", required = false) String uploadType,
      @RequestParam(name = "min_size", required = false) Long minSize,
      @RequestParam(name = "max_size", required = false) Long maxSize,
      @RequestParam(name = "start_time", required = false) String startTime,
      @RequestParam(name = "end_time", required = false) String endTime) {
    return ApiResponse.ok(
        fileService.list(
            page, pageSize, keyword, fileType, status, uploadType, minSize, maxSize, startTime, endTime));
  }

  @DeleteMapping("/admin/files/{id}")
  public ApiResponse<Void> delete(@PathVariable long id) {
    fileService.delete(id);
    return ApiResponse.ok(null);
  }
}
