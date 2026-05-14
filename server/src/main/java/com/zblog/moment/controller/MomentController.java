package com.zblog.moment.controller;

import com.zblog.common.api.ApiResponse;
import com.zblog.common.api.PageResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MomentController {

  @GetMapping("/moments")
  public ApiResponse<PageResponse<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(name = "page_size", defaultValue = "30") int pageSize) {
    return ApiResponse.ok(new PageResponse<>(List.of(), 0, page, pageSize));
  }
}
