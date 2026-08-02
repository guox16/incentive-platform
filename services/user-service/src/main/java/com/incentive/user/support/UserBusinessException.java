package com.incentive.user.support;

import org.springframework.http.HttpStatus;

/** 可预期的用户业务失败，供 HTTP 层映射为稳定的错误响应。 */
public class UserBusinessException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public UserBusinessException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() { return code; }
  public HttpStatus getStatus() { return status; }
}

