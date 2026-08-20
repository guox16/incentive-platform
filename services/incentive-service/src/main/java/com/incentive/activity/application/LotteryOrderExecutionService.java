package com.incentive.activity.application;

import com.incentive.activity.support.IncentiveBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LotteryOrderExecutionService {
  private final LotteryOrderProcessor processor;
  private final LotteryRetryStateService retryStateService;

  public LotteryOrderExecutionService(
      LotteryOrderProcessor processor, LotteryRetryStateService retryStateService) {
    this.processor = processor;
    this.retryStateService = retryStateService;
  }

  public LotteryOrderProcessor.ProcessingResult execute(Long orderId) {
    try {
      return processor.process(orderId);
    } catch (RuntimeException firstFailure) {
      try {
        // 同一订单、同一积分业务号立即续跑一次，覆盖短暂网络抖动和响应丢失。
        return processor.process(orderId);
      } catch (RuntimeException secondFailure) {
        if (firstFailure != secondFailure) firstFailure.addSuppressed(secondFailure);
        LotteryRetryStateService.FailureRecord record = recordFailure(orderId, secondFailure);
        if (record.alreadySucceeded()) {
          return processor.process(orderId);
        }
        if (record.retryScheduled()) {
          IncentiveBusinessException scheduled = new IncentiveBusinessException(
              "LOTTERY_RETRY_SCHEDULED", "抽奖正在处理中，请稍后重试",
              HttpStatus.SERVICE_UNAVAILABLE);
          scheduled.initCause(secondFailure);
          throw scheduled;
        }
        throw secondFailure;
      }
    }
  }

  private LotteryRetryStateService.FailureRecord recordFailure(
      Long orderId, RuntimeException failure) {
    try {
      return retryStateService.recordFailure(orderId, failure);
    } catch (RuntimeException recordingFailure) {
      failure.addSuppressed(recordingFailure);
      throw failure;
    }
  }
}
