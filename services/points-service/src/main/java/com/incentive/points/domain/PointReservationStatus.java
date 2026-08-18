package com.incentive.points.domain;

/** 积分预占的生命周期状态。 */
public enum PointReservationStatus {
  RESERVED,
  CONFIRMED,
  CANCELLED,
  EXPIRED
}
