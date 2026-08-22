package com.incentive.activity.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record LotteryPreDrawRuleResponse(
    String type,
    int executionOrder,
    boolean enabled,
    List<Long> userIds,
    Map<Long, Long> prizeMinimumDrawCounts,
    List<PointsTier> pointsTiers) {

  public record PointsTier(
      long minimumPoints,
      Map<Long, BigDecimal> weightMultipliers) {}
}
