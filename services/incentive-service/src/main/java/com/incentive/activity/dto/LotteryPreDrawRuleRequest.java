package com.incentive.activity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record LotteryPreDrawRuleRequest(
    @NotBlank @Size(max = 64) String type,
    boolean enabled,
    List<Long> userIds,
    Map<Long, Long> prizeMinimumDrawCounts,
    @Valid List<PointsTier> pointsTiers) {

  public record PointsTier(
      long minimumPoints,
      Map<Long, BigDecimal> weightMultipliers) {}
}
