package com.incentive.award.dto;

import com.incentive.award.domain.PrizeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreatePrizeRequest(
    @NotBlank @Size(max = 64) String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull PrizeType type,
    @PositiveOrZero long availableStock,
    String awardPayload) {}
