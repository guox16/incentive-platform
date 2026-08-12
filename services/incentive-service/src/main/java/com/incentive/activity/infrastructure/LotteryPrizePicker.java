package com.incentive.activity.infrastructure;

import com.incentive.activity.domain.LotteryPrize;
import java.util.List;

public interface LotteryPrizePicker {
  Long pick(Long activityId, int ruleVersion, List<LotteryPrize> prizes);
}
