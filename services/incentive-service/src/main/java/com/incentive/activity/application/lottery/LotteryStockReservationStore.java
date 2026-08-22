package com.incentive.activity.application.lottery;

/** 抽奖库存编号存储接口；同一抽奖单重复预占必须返回相同编号。 */
public interface LotteryStockReservationStore {
  Long reserve(Long activityId, Long prizeId, Long lotteryOrderId, long quota);

  void release(Long activityId, Long prizeId, Long lotteryOrderId, long stockNo);

  /** Redis回档产生重复编号时只丢弃本次映射，不能把重复编号放回可用列表。 */
  void discard(Long activityId, Long prizeId, Long lotteryOrderId, long stockNo);
}
