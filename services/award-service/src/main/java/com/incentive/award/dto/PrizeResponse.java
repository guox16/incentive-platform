package com.incentive.award.dto;

import com.incentive.award.domain.PrizeStatus;
import com.incentive.award.domain.PrizeType;
import java.time.Instant;

public record PrizeResponse(Long id, String code, String name, PrizeType type, PrizeStatus status,
    long availableStock, String awardPayload, Instant createdAt, Instant updatedAt) {}
