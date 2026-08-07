package com.incentive.user.support;

import org.springframework.http.HttpStatus;

/** 可预期的用户业务失败，供 HTTP 层映射为稳定的错误响应。 */
public class UserBusinessException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  /** 创建携带业务错误码和 HTTP 状态的异常。 */
  public UserBusinessException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  /** 获取业务错误码。 */
  public String getCode() { return code; }
  /** 获取对应的 HTTP 状态。 */
  public HttpStatus getStatus() { return status; }
}
