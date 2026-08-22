package com.incentive.activity.application.lottery;

import com.incentive.activity.domain.LotteryPoolEntry;
import java.util.List;

public interface LotteryPreDrawRule {
  String type();

  void validateConfiguration(LotteryPreDrawRuleDefinition definition);

  LotteryPreDrawRuleResult apply(
      LotteryPreDrawContext context, List<LotteryPoolEntry> pool,
      LotteryPreDrawRuleDefinition definition);
}
