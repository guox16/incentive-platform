package com.incentive.points.domain;

/** 扣减后余额不能为负时抛出的领域异常。 */
public class InsufficientPointsException extends RuntimeException {
  public InsufficientPointsException() {
    super("积分余额不足");
  }
}
