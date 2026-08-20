package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class LotteryOrderReconciliationServiceTest {
  private static final long ORDER_ID = 7001L;
  private static final long BUSINESS_ID = 9001L;
  @Mock private LotteryOrderRepository orderRepository;
  @Mock private LotteryOrderStateService orderStateService;
  @Mock private LotteryParticipationStateService participationStateService;
  @Mock private LotteryRetryStateService retryStateService;
  @Mock private PointsClient pointsClient;

  @Test
  void confirmedReservationCompletesSavedLotteryResult() {
    AtomicReference<LotteryOrderStatus> state =
        new AtomicReference<>(LotteryOrderStatus.RESULT_SAVED);
    LotteryOrder order = order(state);
    when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(pointsClient.getReservation(BUSINESS_ID)).thenReturn(points("CONFIRMED", 88L));
    doAnswer(invocation -> {
      state.set(LotteryOrderStatus.SUCCESS);
      return null;
    }).when(participationStateService).complete(ORDER_ID, 88L);

    var result = service().reconcile(ORDER_ID);

    assertThat(result).isEqualTo(
        LotteryOrderReconciliationService.ReconciliationResult.COMPLETED);
    verify(participationStateService).complete(ORDER_ID, 88L);
  }

  @Test
  void reservedPointsAreCancelledBeforeLotteryFails() {
    LotteryOrder order = order(new AtomicReference<>(LotteryOrderStatus.POINTS_RESERVED));
    when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(pointsClient.getReservation(BUSINESS_ID)).thenReturn(points("RESERVED", null));
    when(pointsClient.cancelReservation(BUSINESS_ID)).thenReturn(points("CANCELLED", null));
    when(retryStateService.markReconciledFailed(
        ORDER_ID, "POINT_RESERVATION_CANCELLED")).thenReturn(true);

    var result = service().reconcile(ORDER_ID);

    assertThat(result).isEqualTo(LotteryOrderReconciliationService.ReconciliationResult.FAILED);
    verify(retryStateService).markReconciledFailed(
        ORDER_ID, "POINT_RESERVATION_CANCELLED");
  }

  @Test
  void unavailablePointsServiceOnlyDefersReconciliation() {
    LotteryOrder order = order(new AtomicReference<>(LotteryOrderStatus.INIT));
    when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    IncentiveBusinessException failure = new IncentiveBusinessException(
        "POINTS_SERVICE_UNAVAILABLE", "暂不可用", HttpStatus.BAD_GATEWAY);
    when(pointsClient.getReservation(BUSINESS_ID)).thenThrow(failure);

    var result = service().reconcile(ORDER_ID);

    assertThat(result).isEqualTo(
        LotteryOrderReconciliationService.ReconciliationResult.DEFERRED);
    verify(retryStateService).deferReconciliation(ORDER_ID, failure);
  }

  private LotteryOrderReconciliationService service() {
    return new LotteryOrderReconciliationService(
        orderRepository, orderStateService, participationStateService,
        retryStateService, pointsClient);
  }

  private LotteryOrder order(AtomicReference<LotteryOrderStatus> state) {
    LotteryOrder order = mock(LotteryOrder.class);
    when(order.getStatus()).thenAnswer(invocation -> state.get());
    when(order.getPointsBusinessId()).thenReturn(BUSINESS_ID);
    return order;
  }

  private PointsClient.PointReservationResult points(String status, Long transactionId) {
    return new PointsClient.PointReservationResult(
        BUSINESS_ID, 90L, status, transactionId,
        Instant.parse("2026-08-20T00:35:00Z"), false);
  }
}
