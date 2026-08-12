package com.incentive.activity.dto;

import com.incentive.activity.domain.PrizeType;

public record RedemptionItemResponse(
    Long id,
    String itemCode,
    Long prizeId,
    String name,
    PrizeType type,
    String coverUrl,
    long pointsPrice,
    Long campaignQuota,
    int displayOrder) {}
