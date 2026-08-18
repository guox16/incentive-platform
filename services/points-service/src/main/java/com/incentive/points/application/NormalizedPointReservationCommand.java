package com.incentive.points.application;

import java.time.Instant;

/** 经过标准化、可直接进入预占事务的命令。 */
record NormalizedPointReservationCommand(
    Long businessId,
    Long userId,
    long amount,
    String source,
    String remark,
    Instant expiresAt) {}
