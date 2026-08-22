package com.incentive.activity.infrastructure;

import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

public final class WeightedSlotPool {
  private static final int MAX_SLOTS = 100_000;

  private WeightedSlotPool() {}

  public static List<String> build(List<LotteryPoolEntry> pool) {
    if (pool.isEmpty()) {
      throw new IncentiveBusinessException(
          "LOTTERY_POOL_EMPTY", "抽奖奖池未配置", HttpStatus.CONFLICT);
    }
    long divisor = pool.stream().mapToLong(LotteryPoolEntry::weight)
        .reduce(0, WeightedSlotPool::gcd);
    long slots = pool.stream().mapToLong(entry -> entry.weight() / divisor).sum();
    if (slots > MAX_SLOTS) {
      throw new IncentiveBusinessException(
          "LOTTERY_POOL_TOO_LARGE", "抽奖权重约分后的槽位数超过限制", HttpStatus.CONFLICT);
    }
    List<String> result = new ArrayList<>((int) slots);
    for (LotteryPoolEntry entry : pool) {
      long count = entry.weight() / divisor;
      for (long index = 0; index < count; index++) {
        result.add(entry.lotteryPrizeId().toString());
      }
    }
    return result;
  }

  private static long gcd(long left, long right) {
    while (right != 0) {
      long remainder = left % right;
      left = right;
      right = remainder;
    }
    return Math.abs(left);
  }
}
