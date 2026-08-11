package com.incentive.activity.dto;

import com.incentive.activity.domain.PrizeType;
import java.time.Instant;

public record LotteryDrawResponse(
    Long participationId,
    String activityCode,
    Long userId,
    Long prizeId,
    String prizeName,
    PrizeType prizeType,
    String coverUrl,
    boolean won,
    boolean pendingAwardCreated,
    long pointsCost,
    Long pointTransactionId,
    long balanceAfter,
    Instant drawnAt) {}
