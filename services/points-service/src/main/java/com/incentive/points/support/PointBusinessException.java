package com.incentive.points.support;

import org.springframework.http.HttpStatus;

/** 可预期的积分业务失败，携带稳定错误码和 HTTP 状态。 */
public class PointBusinessException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public PointBusinessException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() { return code; }
  public HttpStatus getStatus() { return status; }
}
