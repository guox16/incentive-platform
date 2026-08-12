package com.incentive.award.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdjustInventoryRequest(
    @NotBlank @Size(max = 64) String businessNo,
    @NotNull Long changeAmount,
    @Size(max = 256) String remark) {}
