package com.incentive.activity.domain;

import java.util.Objects;

public record LotteryPoolEntry(LotteryPrize prize, long weight) {
  public LotteryPoolEntry {
    Objects.requireNonNull(prize, "活动奖品不能为空");
    if (weight <= 0) throw new IllegalArgumentException("奖品权重必须大于0");
  }

  public static LotteryPoolEntry original(LotteryPrize prize) {
    return new LotteryPoolEntry(prize, prize.getWeight());
  }

  public Long lotteryPrizeId() {
    return prize.getId();
  }
}
