package com.incentive.activity.application.lottery;

import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.domain.LotteryPrize;
import java.util.List;

public record LotteryPreDrawRuleResult(
    List<LotteryPoolEntry> pool,
    LotteryPrize designatedPrize) {

  public LotteryPreDrawRuleResult {
    pool = List.copyOf(pool);
  }

  public static LotteryPreDrawRuleResult continueWith(List<LotteryPoolEntry> pool) {
    return new LotteryPreDrawRuleResult(pool, null);
  }

  public static LotteryPreDrawRuleResult designate(
      List<LotteryPoolEntry> pool, LotteryPrize prize) {
    return new LotteryPreDrawRuleResult(pool, prize);
  }

  public boolean designated() {
    return designatedPrize != null;
  }
}
