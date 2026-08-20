package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotteryOrderStateService {
  private final LotteryOrderRepository orderRepository;
  private final Clock clock;

  public LotteryOrderStateService(LotteryOrderRepository orderRepository, Clock clock) {
    this.orderRepository = orderRepository;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markPointsReserved(Long orderId, Instant expiresAt, long balanceAfter) {
    LotteryOrder order = orderRepository.findByIdForUpdate(orderId)
        .orElseThrow(() -> new IncentiveBusinessException(
            "LOTTERY_ORDER_NOT_FOUND", "抽奖单不存在", HttpStatus.NOT_FOUND));
    order.markPointsReserved(expiresAt, balanceAfter, clock.instant());
  }
}
