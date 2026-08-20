package com.incentive.activity.application;

import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class LotteryRetryPolicy {
  private static final Set<String> RETRYABLE_CODES = Set.of(
      "POINTS_SERVICE_UNAVAILABLE",
      "POINTS_SERVICE_ERROR",
      "POINTS_SERVICE_INVALID_RESPONSE",
      "POINTS_COMMAND_CONFLICT");

  private final int maxAttempts;
  private final Duration initialDelay;
  private final Duration maxDelay;

  public LotteryRetryPolicy(
      @Value("${lottery.retry.max-attempts:5}") int maxAttempts,
      @Value("${lottery.retry.initial-delay:PT5S}") Duration initialDelay,
      @Value("${lottery.retry.max-delay:PT5M}") Duration maxDelay) {
    if (maxAttempts <= 0 || initialDelay.isNegative() || initialDelay.isZero()
        || maxDelay.compareTo(initialDelay) < 0) {
      throw new IllegalArgumentException("抽奖重试参数不合法");
    }
    this.maxAttempts = maxAttempts;
    this.initialDelay = initialDelay;
    this.maxDelay = maxDelay;
  }

  public Decision decide(Throwable failure, int previousFailures) {
    IncentiveBusinessException businessFailure = findBusinessFailure(failure);
    String code;
    boolean retryable;
    if (businessFailure != null) {
      code = businessFailure.getCode();
      retryable = RETRYABLE_CODES.contains(code);
    } else if (findDataAccessFailure(failure)) {
      code = "DATABASE_TEMPORARY_ERROR";
      retryable = true;
    } else {
      code = "LOTTERY_PROCESSING_ERROR";
      retryable = false;
    }

    int failuresAfterRecord = Math.addExact(previousFailures, 1);
    boolean schedule = retryable && failuresAfterRecord < maxAttempts;
    return new Decision(code, schedule, schedule ? delayFor(previousFailures) : null);
  }

  private Duration delayFor(int previousFailures) {
    Duration delay = initialDelay;
    for (int i = 0; i < previousFailures && delay.compareTo(maxDelay) < 0; i++) {
      if (delay.compareTo(maxDelay.dividedBy(2)) > 0) return maxDelay;
      delay = delay.multipliedBy(2);
    }
    return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
  }

  private IncentiveBusinessException findBusinessFailure(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof IncentiveBusinessException businessException) {
        return businessException;
      }
    }
    return null;
  }

  private boolean findDataAccessFailure(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof DataAccessException) return true;
    }
    return false;
  }

  public record Decision(String failureCode, boolean retryable, Duration delay) {}
}
