package com.incentive.activity.dto;

import com.incentive.activity.domain.PrizeType;
import java.time.Instant;

public record LotteryRecordResponse(
    Long orderId,
    String activityCode,
    String activityName,
    LotteryRecordStatus status,
    Long prizeId,
    String prizeName,
    PrizeType prizeType,
    long pointsCost,
    Instant createdAt,
    Instant updatedAt) {}
