package com.incentive.activity.dto;

import com.incentive.activity.domain.PrizeType;
import java.time.Instant;

public record RedemptionResponse(
    Long redemptionId,
    String activityCode,
    Long itemId,
    String itemCode,
    Long userId,
    Long prizeId,
    String prizeName,
    PrizeType prizeType,
    String coverUrl,
    long pointsCost,
    Long pointTransactionId,
    long balanceAfter,
    boolean pendingAwardCreated,
    Instant redeemedAt) {}
