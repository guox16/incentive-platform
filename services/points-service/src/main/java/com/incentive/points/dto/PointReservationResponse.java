package com.incentive.points.dto;

import com.incentive.points.domain.PointReservationStatus;
import java.time.Instant;

/** 积分预占及其当前状态。 */
public record PointReservationResponse(
    Long id,
    Long businessId,
    Long userId,
    long amount,
    long balanceBefore,
    long balanceAfter,
    String source,
    String remark,
    PointReservationStatus status,
    Long confirmedTransactionId,
    Instant expiresAt,
    Instant confirmedAt,
    Instant cancelledAt,
    Instant expiredAt,
    Instant createdAt,
    Instant updatedAt,
    boolean replayed) {}
