package com.incentive.activity.infrastructure;

import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RedisLotteryPrizePicker implements LotteryPrizePicker {
  private final StringRedisTemplate redis;

  public RedisLotteryPrizePicker(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Long pick(Long activityId, int ruleVersion, List<LotteryPrize> prizes) {
    String key = "incentive:lottery:slots:" + activityId + ":v" + ruleVersion;
    Long size = redis.opsForList().size(key);
    if (size == null || size == 0) {
      rebuild(key, prizes);
      size = redis.opsForList().size(key);
    }
    if (size == null || size == 0) {
      throw new IncentiveBusinessException(
          "LOTTERY_POOL_UNAVAILABLE", "抽奖奖池暂不可用", HttpStatus.SERVICE_UNAVAILABLE);
    }
    String prizeId = redis.opsForList().index(key, ThreadLocalRandom.current().nextLong(size));
    if (prizeId == null) {
      throw new IncentiveBusinessException(
          "LOTTERY_POOL_UNAVAILABLE", "抽奖奖池暂不可用", HttpStatus.SERVICE_UNAVAILABLE);
    }
    return Long.valueOf(prizeId);
  }

  private void rebuild(String key, List<LotteryPrize> prizes) {
    List<String> slots = WeightedSlotPool.build(prizes);
    String temporaryKey = key + ":building:" + UUID.randomUUID();
    redis.opsForList().rightPushAll(temporaryKey, slots);
    redis.rename(temporaryKey, key);
  }
}
