package com.incentive.award.dto;

import java.time.Instant;

public record PrizeInventoryLedgerResponse(Long id, Long prizeId, String businessNo, String operationType,
    long changeAmount, long balanceAfter, String remark, Instant createdAt) {}
