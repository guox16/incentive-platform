package com.incentive.activity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UpdatePrizePoolRequest(
    @NotEmpty(message = "奖池至少需要一个奖品") List<@Valid Entry> prizes) {

  public record Entry(
      @NotNull @Positive Long prizeId,
      @Positive long weight,
      @Positive Long campaignQuota) {}
}
