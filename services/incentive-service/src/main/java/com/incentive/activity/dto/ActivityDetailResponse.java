package com.incentive.activity.dto;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import java.time.Instant;
import java.util.List;

public record ActivityDetailResponse(
    Long id,
    String code,
    ActivityType type,
    String name,
    ActivityStatus status,
    Instant startsAt,
    Instant endsAt,
    int ruleVersion,
    long pointsCost,
    Integer dailyLimit,
    List<LotteryPrizeResponse> prizes,
    List<RedemptionItemResponse> items) {}
