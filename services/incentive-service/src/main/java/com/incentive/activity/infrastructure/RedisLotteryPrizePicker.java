package com.incentive.activity.infrastructure;

import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.support.IncentiveBusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RedisLotteryPrizePicker implements LotteryPrizePicker {
  private static final Duration POOL_TTL = Duration.ofDays(1);
  private final StringRedisTemplate redis;

  public RedisLotteryPrizePicker(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Long pick(Long activityId, int ruleVersion, List<LotteryPoolEntry> pool) {
    String key = "incentive:lottery:slots:" + activityId + ":v" + ruleVersion
        + ":" + fingerprint(pool);
    Long size = redis.opsForList().size(key);
    if (size == null || size == 0) {
      rebuild(key, pool);
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

  private void rebuild(String key, List<LotteryPoolEntry> pool) {
    List<String> slots = WeightedSlotPool.build(pool);
    String temporaryKey = key + ":building:" + UUID.randomUUID();
    redis.opsForList().rightPushAll(temporaryKey, slots);
    redis.rename(temporaryKey, key);
    redis.expire(key, POOL_TTL);
  }

  private String fingerprint(List<LotteryPoolEntry> pool) {
    StringBuilder source = new StringBuilder();
    pool.stream().sorted(java.util.Comparator.comparing(LotteryPoolEntry::lotteryPrizeId))
        .forEach(entry -> source.append(entry.lotteryPrizeId()).append(':')
            .append(entry.weight()).append(';'));
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(source.toString().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest, 0, 12);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("当前运行环境不支持SHA-256", ex);
    }
  }
}
