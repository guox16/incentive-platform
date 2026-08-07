package com.incentive.points.support;

import org.springframework.http.HttpStatus;

/** 可预期的积分业务失败，携带稳定错误码和 HTTP 状态。 */
public class PointBusinessException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  /** 创建携带业务错误码和 HTTP 状态的异常。 */
  public PointBusinessException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  /** 获取业务错误码。 */
  public String getCode() { return code; }
  /** 获取对应的 HTTP 状态。 */
  public HttpStatus getStatus() { return status; }
}
