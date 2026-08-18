package com.incentive.points.application;

/** 并发请求竞争同一积分业务号时触发，由外层在事务回滚后重读既有流水。 */
class PointCommandRaceException extends RuntimeException {
  PointCommandRaceException(Throwable cause) {
    super(cause);
  }
}
