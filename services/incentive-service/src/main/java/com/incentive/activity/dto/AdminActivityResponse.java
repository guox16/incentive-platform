package com.incentive.activity.dto;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import java.time.Instant;

public record AdminActivityResponse(
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
    String qualificationRule,
    Instant createdAt,
    Instant updatedAt) {}
