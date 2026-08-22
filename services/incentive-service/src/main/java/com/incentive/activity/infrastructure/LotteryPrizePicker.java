package com.incentive.activity.infrastructure;

import com.incentive.activity.domain.LotteryPoolEntry;
import java.util.List;

public interface LotteryPrizePicker {
  Long pick(Long activityId, int ruleVersion, List<LotteryPoolEntry> pool);
}
