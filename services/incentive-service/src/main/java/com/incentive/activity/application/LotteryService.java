package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.LotteryDrawResponse;
import com.incentive.activity.infrastructure.PointsClient;
import org.springframework.stereotype.Service;

@Service
public class LotteryService {
  private final LotteryOrderCreationService orderCreationService;
  private final LotteryOrderStateService orderStateService;
  private final LotteryParticipationStateService participationStateService;
  private final PointsClient pointsClient;

  public LotteryService(LotteryOrderCreationService orderCreationService,
      LotteryOrderStateService orderStateService,
      LotteryParticipationStateService participationStateService,
      PointsClient pointsClient) {
    this.orderCreationService = orderCreationService;
    this.orderStateService = orderStateService;
    this.participationStateService = participationStateService;
    this.pointsClient = pointsClient;
  }

  public LotteryDrawResponse draw(String activityCode, Long userId, String requestId) {
    LotteryOrder order = orderCreationService.createOrGet(activityCode, userId, requestId).order();
    PointsClient.PointReservationResult reservation = pointsClient.reserve(
        order.getPointsBusinessId(), userId, order.getPointsCost(),
        "LOTTERY", "参与抽奖：" + order.getActivityCode());
    orderStateService.markPointsReserved(order.getId(), reservation.expiresAt());
    participationStateService.saveWaiting(order.getId());
    PointsClient.PointReservationResult confirmation =
        pointsClient.confirmReservation(order.getPointsBusinessId());
    LotteryParticipationStateService.CompletionResult completion =
        participationStateService.complete(order.getId(), confirmation.confirmedTransactionId());
    LotteryParticipation participation = completion.participation();

    return new LotteryDrawResponse(participation.getId(), order.getActivityCode(), userId,
        order.getPrizeId(), order.getPrizeName(), order.getPrizeType(),
        order.getCoverUrl(), order.getPrizeType() != PrizeType.NONE,
        completion.pendingAwardCreated(),
        order.getPointsCost(), confirmation.confirmedTransactionId(), reservation.balanceAfter(),
        participation.getCreatedAt());
  }
}
