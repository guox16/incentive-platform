package com.incentive.activity.infrastructure;

public record AwardResultMessage(
    Long pendingAwardId,
    String commandKey,
    Status status,
    String resultRef,
    String failureCode,
    String errorMessage) {

  public enum Status { AWARDED, FAILED }
}
