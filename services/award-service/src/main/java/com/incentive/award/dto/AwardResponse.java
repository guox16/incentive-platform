package com.incentive.award.dto;

import com.incentive.award.domain.AwardStatus;
import com.incentive.award.domain.AwardType;
import java.time.Instant;

public record AwardResponse(
    Long id,
    String code,
    String name,
    AwardType type,
    AwardStatus status,
    String coverUrl,
    String awardPayload,
    long totalStock,
    long availableStock,
    Instant createdAt,
    Instant updatedAt) {}
