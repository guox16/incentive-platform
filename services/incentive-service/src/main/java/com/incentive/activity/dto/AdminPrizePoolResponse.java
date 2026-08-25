package com.incentive.activity.dto;

import com.incentive.activity.domain.PrizeType;
import java.util.List;

public record AdminPrizePoolResponse(
    List<ConfiguredPrize> configured,
    List<PrizeCandidate> candidates) {

  public record ConfiguredPrize(
      Long prizeId, String code, String name, PrizeType type, long availableStock,
      long weight, Long campaignQuota, int displayOrder) {}

  public record PrizeCandidate(
      Long prizeId, String code, String name, PrizeType type, long availableStock) {}
}
