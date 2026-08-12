package com.incentive.award.support;

import org.springframework.http.HttpStatus;

public class PrizeBusinessException extends RuntimeException {
  private final String code;
  private final HttpStatus status;
  public PrizeBusinessException(String code, String message, HttpStatus status) {
    super(message); this.code = code; this.status = status;
  }
  public String getCode() { return code; }
  public HttpStatus getStatus() { return status; }
}
