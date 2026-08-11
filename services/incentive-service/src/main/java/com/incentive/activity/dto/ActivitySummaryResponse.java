package com.incentive.activity.dto;

import com.incentive.activity.domain.ActivityType;
import java.time.Instant;

public record ActivitySummaryResponse(
    Long id,
    String code,
    ActivityType type,
    String name,
    Instant startsAt,
    Instant endsAt) {}
