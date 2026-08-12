package com.incentive.award.dto;

import com.incentive.award.domain.PrizeStatus;
import com.incentive.award.domain.PrizeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePrizeRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull PrizeType type,
    @NotNull PrizeStatus status,
    String awardPayload) {}
