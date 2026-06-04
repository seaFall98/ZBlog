package com.zblog.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(int code, String message, T data) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "success", data);
  }

  public static <T> ApiResponse<T> failure(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
