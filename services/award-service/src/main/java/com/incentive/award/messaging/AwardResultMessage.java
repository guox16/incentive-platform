package com.incentive.award.messaging;

public record AwardResultMessage(
    Long pendingAwardId,
    String commandKey,
    Status status,
    String resultRef,
    String failureCode,
    String errorMessage) {

  public enum Status { AWARDED, FAILED }

  public static AwardResultMessage awarded(
      AwardCommandMessage command, String resultRef) {
    return new AwardResultMessage(
        command.pendingAwardId(), command.commandKey(), Status.AWARDED,
        resultRef, null, null);
  }

  public static AwardResultMessage failed(
      AwardCommandMessage command, String code, String message) {
    return new AwardResultMessage(
        command.pendingAwardId(), command.commandKey(), Status.FAILED,
        null, code, message);
  }
}
