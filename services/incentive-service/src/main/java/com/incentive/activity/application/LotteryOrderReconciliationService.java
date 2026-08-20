package com.incentive.activity.application;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 按积分侧既有事实收敛异常抽奖单，不重新发起预占或确认命令。 */
@Service
public class LotteryOrderReconciliationService {
  private final LotteryOrderRepository orderRepository;
  private final LotteryOrderStateService orderStateService;
  private final LotteryParticipationStateService participationStateService;
  private final LotteryRetryStateService retryStateService;
  private final PointsClient pointsClient;

  public LotteryOrderReconciliationService(LotteryOrderRepository orderRepository,
      LotteryOrderStateService orderStateService,
      LotteryParticipationStateService participationStateService,
      LotteryRetryStateService retryStateService, PointsClient pointsClient) {
    this.orderRepository = orderRepository;
    this.orderStateService = orderStateService;
    this.participationStateService = participationStateService;
    this.retryStateService = retryStateService;
    this.pointsClient = pointsClient;
  }

  public ReconciliationResult reconcile(Long orderId) {
    LotteryOrder order = loadOrder(orderId);
    if (order.getStatus() == LotteryOrderStatus.SUCCESS) return ReconciliationResult.COMPLETED;
    if (order.getStatus() == LotteryOrderStatus.FAILED) return ReconciliationResult.FAILED;

    try {
      PointsClient.PointReservationResult points =
          pointsClient.getReservation(order.getPointsBusinessId());
      validateBusinessId(order, points);
      return reconcileKnownState(orderId, points);
    } catch (IncentiveBusinessException failure) {
      if ("POINT_RESERVATION_NOT_FOUND".equals(failure.getCode())) {
        return fail(orderId, failure.getCode());
      }
      retryStateService.deferReconciliation(orderId, failure);
      return ReconciliationResult.DEFERRED;
    } catch (RuntimeException failure) {
      retryStateService.deferReconciliation(orderId, failure);
      return ReconciliationResult.DEFERRED;
    }
  }

  private ReconciliationResult reconcileKnownState(
      Long orderId, PointsClient.PointReservationResult points) {
    return switch (points.status()) {
      case "CONFIRMED" -> completeConfirmed(orderId, points);
      case "RESERVED" -> cancelReserved(orderId, points.businessId());
      case "CANCELLED" -> fail(orderId, "POINT_RESERVATION_CANCELLED");
      case "EXPIRED" -> fail(orderId, "POINT_RESERVATION_EXPIRED");
      default -> throw invalidResponse("无法识别的积分预占状态: " + points.status());
    };
  }

  private ReconciliationResult cancelReserved(Long orderId, Long businessId) {
    PointsClient.PointReservationResult cancelled = pointsClient.cancelReservation(businessId);
    if (!businessId.equals(cancelled.businessId())) {
      throw invalidResponse("积分取消结果的业务号不一致");
    }
    if ("CONFIRMED".equals(cancelled.status())) {
      return completeConfirmed(orderId, cancelled);
    }
    if (!"CANCELLED".equals(cancelled.status()) && !"EXPIRED".equals(cancelled.status())) {
      throw invalidResponse("积分预占未成功取消");
    }
    return fail(orderId, "EXPIRED".equals(cancelled.status())
        ? "POINT_RESERVATION_EXPIRED" : "POINT_RESERVATION_CANCELLED");
  }

  private ReconciliationResult completeConfirmed(
      Long orderId, PointsClient.PointReservationResult points) {
    if (points.confirmedTransactionId() == null || points.expiresAt() == null) {
      throw invalidResponse("已确认积分缺少扣减流水或预占时间");
    }
    for (int step = 0; step < 4; step++) {
      LotteryOrder order = loadOrder(orderId);
      if (order.getStatus() == LotteryOrderStatus.INIT) {
        orderStateService.markPointsReserved(orderId, points.expiresAt(), points.balanceAfter());
        continue;
      }
      if (order.getStatus() == LotteryOrderStatus.POINTS_RESERVED) {
        participationStateService.saveWaiting(orderId);
        continue;
      }
      if (order.getStatus() == LotteryOrderStatus.RESULT_SAVED) {
        participationStateService.complete(orderId, points.confirmedTransactionId());
        continue;
      }
      if (order.getStatus() == LotteryOrderStatus.SUCCESS) {
        return ReconciliationResult.COMPLETED;
      }
      return ReconciliationResult.FAILED;
    }
    throw invalidResponse("已确认抽奖单无法完成状态收敛");
  }

  private ReconciliationResult fail(Long orderId, String code) {
    return retryStateService.markReconciledFailed(orderId, code)
        ? ReconciliationResult.FAILED : ReconciliationResult.COMPLETED;
  }

  private LotteryOrder loadOrder(Long orderId) {
    return orderRepository.findById(orderId)
        .orElseThrow(() -> new IncentiveBusinessException(
            "LOTTERY_ORDER_NOT_FOUND", "抽奖单不存在", HttpStatus.NOT_FOUND));
  }

  private void validateBusinessId(
      LotteryOrder order, PointsClient.PointReservationResult points) {
    if (points == null || !order.getPointsBusinessId().equals(points.businessId())) {
      throw invalidResponse("积分查询结果的业务号不一致");
    }
  }

  private IncentiveBusinessException invalidResponse(String message) {
    return new IncentiveBusinessException(
        "POINTS_SERVICE_INVALID_RESPONSE", message, HttpStatus.BAD_GATEWAY);
  }

  public enum ReconciliationResult { COMPLETED, FAILED, DEFERRED }
}
