package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryOrderProcessorTest {
  private static final Instant NOW = Instant.parse("2026-08-20T00:30:00Z");
  private static final Instant EXPIRES_AT = NOW.plusSeconds(300);
  @Mock private LotteryOrderRepository orderRepository;
  @Mock private LotteryParticipationRepository participationRepository;
  @Mock private LotteryOrderStateService orderStateService;
  @Mock private LotteryParticipationStateService participationStateService;
  @Mock private PointsClient pointsClient;
  private LotteryOrderProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new LotteryOrderProcessor(orderRepository, participationRepository,
        orderStateService, participationStateService, pointsClient);
  }

  @Test
  void executesAllNodesFromInitInOrder() {
    LotteryOrder order = order();
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    when(orderRepository.findById(7001L)).thenReturn(Optional.of(order));
    when(pointsClient.reserve(9001L, 7L, 10L, "LOTTERY", "参与抽奖：SUMMER_LOTTERY"))
        .thenReturn(reservation("RESERVED", null, false));
    org.mockito.Mockito.doAnswer(invocation -> {
      order.markPointsReserved(EXPIRES_AT, 90L, NOW);
      return null;
    }).when(orderStateService).markPointsReserved(7001L, EXPIRES_AT, 90L);
    when(participationStateService.saveWaiting(7001L)).thenAnswer(invocation -> {
      order.markResultSaved(NOW);
      return participation;
    });
    when(pointsClient.confirmReservation(9001L))
        .thenReturn(reservation("CONFIRMED", 44L, false));
    when(participationStateService.complete(7001L, 44L)).thenAnswer(invocation -> {
      participation.markSuccess(44L, NOW);
      order.markSuccess(NOW);
      return new LotteryParticipationStateService.CompletionResult(participation, true);
    });
    when(participationRepository.findByLotteryOrderId(7001L))
        .thenReturn(Optional.of(participation));

    var result = processor.process(7001L);

    assertThat(result.order().getStatus()).isEqualTo(LotteryOrderStatus.SUCCESS);
    assertThat(result.participation().getPointTransactionId()).isEqualTo(44L);
    assertThat(result.pointsResult().businessId()).isEqualTo(9001L);
    InOrder flow = inOrder(pointsClient, orderStateService, participationStateService);
    flow.verify(pointsClient).reserve(9001L, 7L, 10L,
        "LOTTERY", "参与抽奖：SUMMER_LOTTERY");
    flow.verify(orderStateService).markPointsReserved(7001L, EXPIRES_AT, 90L);
    flow.verify(participationStateService).saveWaiting(7001L);
    flow.verify(pointsClient).confirmReservation(9001L);
    flow.verify(participationStateService).complete(7001L, 44L);
  }

  @Test
  void successfulOrderOnlyQueriesExistingPointsResult() {
    LotteryOrder order = order();
    order.markPointsReserved(EXPIRES_AT, 90L, NOW);
    order.markResultSaved(NOW);
    order.markSuccess(NOW);
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    participation.markSuccess(44L, NOW);
    when(orderRepository.findById(7001L)).thenReturn(Optional.of(order));
    when(participationRepository.findByLotteryOrderId(7001L))
        .thenReturn(Optional.of(participation));

    var result = processor.process(7001L);

    assertThat(result.participation().getId()).isEqualTo(51L);
    verify(pointsClient, never()).reserve(9001L, 7L, 10L,
        "LOTTERY", "参与抽奖：SUMMER_LOTTERY");
    verify(pointsClient, never()).confirmReservation(9001L);
    assertThat(result.pointsResult().balanceAfter()).isEqualTo(90L);
  }

  private PointsClient.PointReservationResult reservation(
      String status, Long transactionId, boolean replayed) {
    return new PointsClient.PointReservationResult(
        9001L, 90L, status, transactionId, EXPIRES_AT, replayed);
  }

  private LotteryOrder order() {
    IncentiveActivity activity = BeanUtils.instantiateClass(IncentiveActivity.class);
    ReflectionTestUtils.setField(activity, "id", 1L);
    ReflectionTestUtils.setField(activity, "code", "SUMMER_LOTTERY");
    ReflectionTestUtils.setField(activity, "type", ActivityType.LOTTERY);
    ReflectionTestUtils.setField(activity, "status", ActivityStatus.ACTIVE);
    ParticipationRule rule = BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    ReflectionTestUtils.setField(rule, "pointsCost", 10L);
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", 31L);
    ReflectionTestUtils.setField(prize, "prizeId", 131L);
    ReflectionTestUtils.setField(prize, "prizeName", "优惠券");
    ReflectionTestUtils.setField(prize, "prizeType", PrizeType.VIRTUAL);
    return new LotteryOrder(7001L, "request-1", 7L, activity, rule, prize,
        9001L, "{\"passed\":true}", NOW);
  }
}
