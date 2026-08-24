package com.incentive.award.application;

public class AwardCommandConflictException extends RuntimeException {
  public AwardCommandConflictException(String message) {
    super(message);
  }
}
