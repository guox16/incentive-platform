package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.repository.LotteryOrderRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotteryRetryStateService {
  private final LotteryOrderRepository orderRepository;
  private final LotteryRetryPolicy retryPolicy;
  private final Clock clock;

  public LotteryRetryStateService(LotteryOrderRepository orderRepository,
      LotteryRetryPolicy retryPolicy, Clock clock) {
    this.orderRepository = orderRepository;
    this.retryPolicy = retryPolicy;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public FailureRecord recordFailure(Long orderId, Throwable failure) {
    LotteryOrder order = orderRepository.findByIdForUpdate(orderId).orElse(null);
    if (order == null) {
      return new FailureRecord(false, false, false, null, null);
    }
    if (order.getStatus() == LotteryOrderStatus.SUCCESS) {
      return new FailureRecord(true, false, false, null, null);
    }
    if (order.getStatus() == LotteryOrderStatus.FAILED) {
      return new FailureRecord(false, true, false, order.getFailureCode(), null);
    }

    Instant now = clock.instant();
    LotteryRetryPolicy.Decision decision = retryPolicy.decide(failure, order.getRetryCount());
    if (decision.retryable()) {
      Instant retryAt = now.plus(decision.delay());
      order.scheduleRetry(decision.failureCode(), retryAt, now);
      return new FailureRecord(false, false, true, decision.failureCode(), retryAt);
    }

    order.markFailed(decision.failureCode(), now);
    return new FailureRecord(false, true, false, decision.failureCode(), null);
  }

  public record FailureRecord(
      boolean alreadySucceeded,
      boolean terminal,
      boolean retryScheduled,
      String failureCode,
      Instant nextRetryAt) {}
}
