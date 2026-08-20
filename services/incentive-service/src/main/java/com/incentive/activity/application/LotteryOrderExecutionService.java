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
    } catch (RuntimeException failure) {
      LotteryRetryStateService.FailureRecord record = recordFailure(orderId, failure);
      if (record.alreadySucceeded()) {
        return processor.process(orderId);
      }
      if (record.retryScheduled()) {
        IncentiveBusinessException scheduled = new IncentiveBusinessException(
            "LOTTERY_RETRY_SCHEDULED", "抽奖正在处理中，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);
        scheduled.initCause(failure);
        throw scheduled;
      }
      throw failure;
    }
  }

  public AutomaticExecutionResult executeAutomatically(Long orderId) {
    try {
      processor.process(orderId);
      return new AutomaticExecutionResult(true, false, false, null);
    } catch (RuntimeException failure) {
      LotteryRetryStateService.FailureRecord record = recordFailure(orderId, failure);
      if (record.alreadySucceeded()) {
        processor.process(orderId);
        return new AutomaticExecutionResult(true, false, false, null);
      }
      return new AutomaticExecutionResult(
          false, record.retryScheduled(), record.terminal(), record.failureCode());
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

  public record AutomaticExecutionResult(
      boolean completed, boolean rescheduled, boolean terminal, String failureCode) {}
}
