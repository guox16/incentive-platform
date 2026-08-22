package com.incentive.activity.application;

import com.incentive.activity.application.lottery.LotteryPostDrawStockRule;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.repository.LotteryOrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotteryRetryStateService {
  private final LotteryOrderRepository orderRepository;
  private final LotteryRetryPolicy retryPolicy;
  private final LotteryPostDrawStockRule postDrawStockRule;
  private final Clock clock;
  private final Duration reconciliationDelay;

  public LotteryRetryStateService(LotteryOrderRepository orderRepository,
      LotteryRetryPolicy retryPolicy, LotteryPostDrawStockRule postDrawStockRule, Clock clock,
      @Value("${lottery.retry.reconciliation-delay:PT5S}") Duration reconciliationDelay) {
    if (reconciliationDelay.isNegative() || reconciliationDelay.isZero()) {
      throw new IllegalArgumentException("抽奖对账延迟必须大于0");
    }
    this.orderRepository = orderRepository;
    this.retryPolicy = retryPolicy;
    this.postDrawStockRule = postDrawStockRule;
    this.clock = clock;
    this.reconciliationDelay = reconciliationDelay;
  }

  /** 对账暂时无法取得积分状态时延后再次查询，不再执行业务命令。 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deferReconciliation(Long orderId, Throwable failure) {
    LotteryOrder order = orderRepository.findByIdForUpdate(orderId).orElse(null);
    if (order == null || order.getStatus() == LotteryOrderStatus.SUCCESS
        || order.getStatus() == LotteryOrderStatus.FAILED) return;
    LotteryRetryPolicy.Decision decision = retryPolicy.decide(failure);
    Instant now = clock.instant();
    order.scheduleRetry(decision.failureCode(), now.plus(reconciliationDelay), now);
  }

  /** 只有积分已取消、已过期或确认不存在后，才把抽奖单置为失败。 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean markReconciledFailed(Long orderId, String failureCode) {
    LotteryOrder order = orderRepository.findByIdForUpdate(orderId).orElse(null);
    if (order == null || order.getStatus() == LotteryOrderStatus.SUCCESS) return false;
    if (order.getStatus() != LotteryOrderStatus.FAILED) {
      releaseStock(order);
      order.markFailed(failureCode, clock.instant());
    }
    return true;
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
    LotteryRetryPolicy.Decision decision = retryPolicy.decide(failure);
    // 已发生积分侧动作的订单必须先对账；INIT 的暂态异常也可能是响应丢失。
    if (decision.transientFailure() || order.getStatus() != LotteryOrderStatus.INIT) {
      Instant retryAt = now.plus(reconciliationDelay);
      order.scheduleRetry(decision.failureCode(), retryAt, now);
      return new FailureRecord(false, false, true, decision.failureCode(), retryAt);
    }

    releaseStock(order);
    order.markFailed(decision.failureCode(), now);
    return new FailureRecord(false, true, false, decision.failureCode(), null);
  }

  public record FailureRecord(
      boolean alreadySucceeded,
      boolean terminal,
      boolean retryScheduled,
      String failureCode,
      Instant nextRetryAt) {}

  private void releaseStock(LotteryOrder order) {
    postDrawStockRule.release(order.getId(), order.getActivityId(), order.getPrizeId(),
        order.getStockNo());
  }
}
