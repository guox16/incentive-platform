package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.LotteryDrawResponse;
import org.springframework.stereotype.Service;

@Service
public class LotteryService {
  private final LotteryOrderCreationService orderCreationService;
  private final LotteryOrderExecutionService executionService;

  public LotteryService(LotteryOrderCreationService orderCreationService,
      LotteryOrderExecutionService executionService) {
    this.orderCreationService = orderCreationService;
    this.executionService = executionService;
  }

  public LotteryDrawResponse draw(String activityCode, Long userId, String requestId) {
    LotteryOrder order = orderCreationService.createOrGet(activityCode, userId, requestId).order();
    LotteryOrderProcessor.ProcessingResult result = executionService.execute(order.getId());

    return new LotteryDrawResponse(result.participation().getId(), order.getActivityCode(), userId,
        order.getPrizeId(), order.getPrizeName(), order.getPrizeType(), order.getCoverUrl(),
        order.getPrizeType() != PrizeType.NONE,
        result.pendingAwardCreated(), order.getPointsCost(),
        result.pointsResult().confirmedTransactionId(), result.pointsResult().balanceAfter(),
        result.participation().getCreatedAt());
  }
}
