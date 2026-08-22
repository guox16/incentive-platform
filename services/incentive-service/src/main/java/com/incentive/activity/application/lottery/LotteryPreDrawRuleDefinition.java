package com.incentive.activity.application.lottery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record LotteryPreDrawRuleDefinition(
    String type,
    int executionOrder,
    boolean enabled,
    Parameters parameters) {

  public sealed interface Parameters
      permits UserListParameters, PrizeUnlockParameters, PointsWeightParameters,
      LuckyFallbackParameters {}

  public record UserListParameters(Set<Long> userIds) implements Parameters {
    public UserListParameters { userIds = Set.copyOf(userIds); }
  }

  public record PrizeUnlockParameters(Map<Long, Long> minimumDrawCounts) implements Parameters {
    public PrizeUnlockParameters { minimumDrawCounts = Map.copyOf(minimumDrawCounts); }
  }

  public record PointsWeightParameters(List<PointsTier> tiers) implements Parameters {
    public PointsWeightParameters { tiers = List.copyOf(tiers); }
  }

  public record LuckyFallbackParameters(Long prizeId) implements Parameters {}

  public record PointsTier(long minimumPoints, Map<Long, BigDecimal> weightMultipliers) {
    public PointsTier { weightMultipliers = Map.copyOf(weightMultipliers); }
  }
}
