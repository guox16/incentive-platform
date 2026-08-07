package com.incentive.points.dto;

import com.incentive.points.domain.PointTransactionType;
import java.time.Instant;
import java.util.UUID;

public record PointTransactionResponse(
    UUID transactionId,
    UUID businessId,
    Long userId,
    PointTransactionType type,
    long amount,
    long balanceBefore,
    long balanceAfter,
    String source,
    String remark,
    Instant createdAt,
    boolean replayed) {}
