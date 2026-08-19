package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.LotteryDrawResponse;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LotteryService {
  private final LotteryOrderCreationService orderCreationService;
  private final LotteryOrderStateService orderStateService;
  private final LotteryParticipationRepository participationRepository;
  private final PendingAwardRepository pendingAwardRepository;
  private final PointsClient pointsClient;
  private final Clock clock;

  public LotteryService(LotteryOrderCreationService orderCreationService,
      LotteryOrderStateService orderStateService,
      LotteryParticipationRepository participationRepository,
      PendingAwardRepository pendingAwardRepository, PointsClient pointsClient, Clock clock) {
    this.orderCreationService = orderCreationService;
    this.orderStateService = orderStateService;
    this.participationRepository = participationRepository;
    this.pendingAwardRepository = pendingAwardRepository;
    this.pointsClient = pointsClient;
    this.clock = clock;
  }

  @Transactional
  public LotteryDrawResponse draw(String activityCode, Long userId, String requestId) {
    LotteryOrder order = orderCreationService.createOrGet(activityCode, userId, requestId).order();
    Instant now = clock.instant();
    PointsClient.PointReservationResult reservation = pointsClient.reserve(
        order.getPointsBusinessId(), userId, order.getPointsCost(),
        "LOTTERY", "参与抽奖：" + order.getActivityCode());
    orderStateService.markPointsReserved(order.getId(), reservation.expiresAt());
    PointsClient.PointReservationResult confirmation =
        pointsClient.confirmReservation(order.getPointsBusinessId());
    LotteryParticipation participation = participationRepository.saveAndFlush(
        new LotteryParticipation(order, confirmation.confirmedTransactionId(), now));
    boolean createsPendingAward = order.getPrizeType() != PrizeType.NONE;
    if (createsPendingAward) {
      pendingAwardRepository.save(PendingAward.forLottery(participation, now));
    }

    return new LotteryDrawResponse(participation.getId(), order.getActivityCode(), userId,
        order.getPrizeId(), order.getPrizeName(), order.getPrizeType(),
        order.getCoverUrl(), order.getPrizeType() != PrizeType.NONE, createsPendingAward,
        order.getPointsCost(), confirmation.confirmedTransactionId(), reservation.balanceAfter(), now);
  }
}
