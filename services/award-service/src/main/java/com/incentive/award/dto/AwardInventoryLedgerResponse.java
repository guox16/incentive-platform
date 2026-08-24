package com.incentive.award.dto;

import java.time.Instant;

public record AwardInventoryLedgerResponse(
    Long id,
    Long awardId,
    String businessNo,
    String operationType,
    long changeAmount,
    long availableAfter,
    String remark,
    Instant createdAt) {}
