package com.incentive.activity.infrastructure;

import com.incentive.activity.application.lottery.LotteryStockReservationStore;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RedisLotteryStockReservationStore implements LotteryStockReservationStore {
  private static final long SOLD_OUT = 0L;
  private static final long QUOTA_MISMATCH = -1L;
  private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
      local existing = redis.call('HGET', KEYS[2], ARGV[1])
      if existing then
        return tonumber(existing)
      end
      local initializedQuota = redis.call('GET', KEYS[3])
      if initializedQuota and tonumber(initializedQuota) ~= tonumber(ARGV[2]) then
        return -1
      end
      if not initializedQuota then
        local quota = tonumber(ARGV[2])
        for stockNo = 1, quota do
          redis.call('RPUSH', KEYS[1], stockNo)
        end
        redis.call('SET', KEYS[3], ARGV[2])
      end
      local stockNo = redis.call('LPOP', KEYS[1])
      if not stockNo then
        return 0
      end
      redis.call('HSET', KEYS[2], ARGV[1], stockNo)
      return tonumber(stockNo)
      """, Long.class);
  private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
      local existing = redis.call('HGET', KEYS[2], ARGV[1])
      if not existing or tonumber(existing) ~= tonumber(ARGV[2]) then
        return 0
      end
      redis.call('HDEL', KEYS[2], ARGV[1])
      redis.call('RPUSH', KEYS[1], ARGV[2])
      return 1
      """, Long.class);
  private static final DefaultRedisScript<Long> DISCARD_SCRIPT = new DefaultRedisScript<>("""
      local existing = redis.call('HGET', KEYS[1], ARGV[1])
      if not existing or tonumber(existing) ~= tonumber(ARGV[2]) then
        return 0
      end
      redis.call('HDEL', KEYS[1], ARGV[1])
      return 1
      """, Long.class);

  private final StringRedisTemplate redis;

  public RedisLotteryStockReservationStore(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Long reserve(Long activityId, Long prizeId, Long lotteryOrderId, long quota) {
    if (quota <= 0) return null;
    Keys keys = keys(activityId, prizeId);
    try {
      Long result = redis.execute(RESERVE_SCRIPT,
          List.of(keys.available(), keys.reservations(), keys.initialized()),
          lotteryOrderId.toString(), Long.toString(quota));
      if (result == null) throw unavailable("Redis未返回库存预占结果", null);
      if (result == SOLD_OUT) return null;
      if (result == QUOTA_MISMATCH) {
        throw new IncentiveBusinessException(
            "LOTTERY_STOCK_QUOTA_CHANGED", "活动发布后不能直接修改奖品库存配额", HttpStatus.CONFLICT);
      }
      return result;
    } catch (IncentiveBusinessException ex) {
      throw ex;
    } catch (DataAccessException ex) {
      throw unavailable("抽奖库存暂不可用", ex);
    }
  }

  @Override
  public void release(
      Long activityId, Long prizeId, Long lotteryOrderId, long stockNo) {
    Keys keys = keys(activityId, prizeId);
    try {
      redis.execute(RELEASE_SCRIPT,
          List.of(keys.available(), keys.reservations()),
          lotteryOrderId.toString(), Long.toString(stockNo));
    } catch (DataAccessException ex) {
      throw unavailable("抽奖库存释放失败", ex);
    }
  }

  @Override
  public void discard(Long activityId, Long prizeId, Long lotteryOrderId, long stockNo) {
    Keys keys = keys(activityId, prizeId);
    try {
      redis.execute(DISCARD_SCRIPT, List.of(keys.reservations()),
          lotteryOrderId.toString(), Long.toString(stockNo));
    } catch (DataAccessException ex) {
      throw unavailable("重复库存编号清理失败", ex);
    }
  }

  private Keys keys(Long activityId, Long prizeId) {
    String prefix = "incentive:lottery:stock:" + activityId + ":" + prizeId;
    return new Keys(prefix + ":available", prefix + ":reservations", prefix + ":initialized");
  }

  private IncentiveBusinessException unavailable(String message, Throwable cause) {
    IncentiveBusinessException exception = new IncentiveBusinessException(
        "LOTTERY_STOCK_UNAVAILABLE", message, HttpStatus.SERVICE_UNAVAILABLE);
    if (cause != null) exception.initCause(cause);
    return exception;
  }

  private record Keys(String available, String reservations, String initialized) {}
}
