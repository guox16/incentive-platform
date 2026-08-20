package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LotteryOrderProcessor {
  private final LotteryOrderRepository orderRepository;
  private final LotteryParticipationRepository participationRepository;
  private final LotteryOrderStateService orderStateService;
  private final LotteryParticipationStateService participationStateService;
  private final PointsClient pointsClient;

  public LotteryOrderProcessor(LotteryOrderRepository orderRepository,
      LotteryParticipationRepository participationRepository,
      LotteryOrderStateService orderStateService,
      LotteryParticipationStateService participationStateService,
      PointsClient pointsClient) {
    this.orderRepository = orderRepository;
    this.participationRepository = participationRepository;
    this.orderStateService = orderStateService;
    this.participationStateService = participationStateService;
    this.pointsClient = pointsClient;
  }

  /** 从抽奖单当前持久化状态继续执行，所有远程命令均使用固定业务号。 */
  public ProcessingResult process(Long orderId) {
    PointsClient.PointReservationResult pointsResult = null;

    for (int step = 0; step < 4; step++) {
      LotteryOrder order = loadOrder(orderId);
      if (order.getStatus() == LotteryOrderStatus.INIT) {
        pointsResult = pointsClient.reserve(
            order.getPointsBusinessId(), order.getUserId(), order.getPointsCost(),
            "LOTTERY", "参与抽奖：" + order.getActivityCode());
        validateBusinessId(order, pointsResult);
        if (!"RESERVED".equals(pointsResult.status())
            && !"CONFIRMED".equals(pointsResult.status())) {
          throw invalidPointsResponse("积分预占状态不允许继续抽奖");
        }
        orderStateService.markPointsReserved(
            orderId, pointsResult.expiresAt(), pointsResult.balanceAfter());
        continue;
      }

      if (order.getStatus() == LotteryOrderStatus.POINTS_RESERVED) {
        participationStateService.saveWaiting(orderId);
        continue;
      }

      if (order.getStatus() == LotteryOrderStatus.RESULT_SAVED) {
        pointsResult = pointsClient.confirmReservation(order.getPointsBusinessId());
        validateBusinessId(order, pointsResult);
        if (!"CONFIRMED".equals(pointsResult.status())
            || pointsResult.confirmedTransactionId() == null) {
          throw invalidPointsResponse("积分确认结果不完整");
        }
        participationStateService.complete(orderId, pointsResult.confirmedTransactionId());
        continue;
      }

      if (order.getStatus() == LotteryOrderStatus.SUCCESS) {
        LotteryParticipation participation = participationRepository.findByLotteryOrderId(orderId)
            .orElseThrow(() -> invalidState("成功抽奖单缺少抽奖记录"));
        if (pointsResult == null) {
          pointsResult = new PointsClient.PointReservationResult(
              order.getPointsBusinessId(), requireBalanceSnapshot(order), "CONFIRMED",
              participation.getPointTransactionId(), order.getPointsReservationExpiresAt(), true);
        }
        if (!"CONFIRMED".equals(pointsResult.status())
            || !participation.getPointTransactionId().equals(
                pointsResult.confirmedTransactionId())) {
          throw invalidPointsResponse("抽奖记录与积分确认结果不一致");
        }
        return new ProcessingResult(order, participation, pointsResult,
            order.getPrizeType() != PrizeType.NONE);
      }

      throw new IncentiveBusinessException(
          "LOTTERY_ORDER_FAILED", "抽奖处理已终止", HttpStatus.CONFLICT);
    }

    throw invalidState("抽奖状态流转次数超出预期");
  }

  private LotteryOrder loadOrder(Long orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new IncentiveBusinessException(
            "LOTTERY_ORDER_NOT_FOUND", "抽奖单不存在", HttpStatus.NOT_FOUND));
  }

  private void validateBusinessId(
      LotteryOrder order, PointsClient.PointReservationResult result) {
    if (result == null || !order.getPointsBusinessId().equals(result.businessId())) {
      throw invalidPointsResponse("积分服务返回的业务号不一致");
    }
  }

  private long requireBalanceSnapshot(LotteryOrder order) {
    if (order.getPointsBalanceAfter() == null) {
      throw invalidState("成功抽奖单缺少积分余额快照");
    }
    return order.getPointsBalanceAfter();
  }

  private IncentiveBusinessException invalidPointsResponse(String message) {
    return new IncentiveBusinessException(
        "POINTS_SERVICE_INVALID_RESPONSE", message, HttpStatus.BAD_GATEWAY);
  }

  private IncentiveBusinessException invalidState(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_ORDER_INVALID_STATE", message, HttpStatus.CONFLICT);
  }

  public record ProcessingResult(
      LotteryOrder order,
      LotteryParticipation participation,
      PointsClient.PointReservationResult pointsResult,
      boolean pendingAwardCreated) {}
}
