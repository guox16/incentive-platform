package com.incentive.activity.dto;

import com.incentive.activity.domain.PrizeType;

public record LotteryPrizeResponse(
    Long id,
    Long prizeId,
    String name,
    PrizeType type,
    String coverUrl,
    Long campaignQuota,
    int displayOrder) {}
