package com.incentive.award.infrastructure;

public class AwardDeliveryException extends RuntimeException {
  private final String code;

  public AwardDeliveryException(String code, String message) {
    super(message);
    this.code = code;
  }

  public AwardDeliveryException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String getCode() { return code; }
}
