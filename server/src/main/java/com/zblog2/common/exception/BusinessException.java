package com.zblog2.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

  private final int code;
  private final HttpStatus status;

  public BusinessException(int code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public int code() {
    return code;
  }

  public HttpStatus status() {
    return status;
  }
}
