package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.infrastructure.PointsClient;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");
  @Mock private LotteryOrderCreationService orderCreationService;
  @Mock private LotteryOrderStateService orderStateService;
  @Mock private LotteryParticipationStateService participationStateService;
  @Mock private PointsClient pointsClient;
  private LotteryService service;

  @BeforeEach
  void setUp() {
    service = new LotteryService(orderCreationService, orderStateService,
        participationStateService, pointsClient);
  }

  @Test
  void winningPrizeUsesOrderFixedResultAndPointsBusinessId() {
    LotteryOrder order = order(PrizeType.VIRTUAL);
    arrange(order);

    var response = service.draw("SUMMER_LOTTERY", 7L, "request-1");

    assertThat(response.won()).isTrue();
    assertThat(response.pendingAwardCreated()).isTrue();
    assertThat(response.balanceAfter()).isEqualTo(90);
    verify(pointsClient).reserve(9001L, 7L, 10L, "LOTTERY", "参与抽奖：SUMMER_LOTTERY");
    verify(orderStateService).markPointsReserved(7001L, NOW.plusSeconds(300));
    verify(pointsClient).confirmReservation(9001L);
    verify(participationStateService).complete(7001L, 44L);

    InOrder flow = inOrder(pointsClient, orderStateService, participationStateService);
    flow.verify(pointsClient).reserve(9001L, 7L, 10L, "LOTTERY", "参与抽奖：SUMMER_LOTTERY");
    flow.verify(orderStateService).markPointsReserved(7001L, NOW.plusSeconds(300));
    flow.verify(participationStateService).saveWaiting(7001L);
    flow.verify(pointsClient).confirmReservation(9001L);
    flow.verify(participationStateService).complete(7001L, 44L);
  }

  @Test
  void nonePrizeOnlyPersistsParticipation() {
    LotteryOrder order = order(PrizeType.NONE);
    arrange(order);

    var response = service.draw("SUMMER_LOTTERY", 7L, "request-1");

    assertThat(response.won()).isFalse();
    assertThat(response.pendingAwardCreated()).isFalse();
  }

  @Test
  void retriesSameFlowAfterConfirmationWasInterrupted() {
    LotteryOrder order = order(PrizeType.VIRTUAL);
    arrange(order);
    when(pointsClient.confirmReservation(9001L))
        .thenThrow(new IllegalStateException("模拟确认积分时断网"))
        .thenReturn(new PointsClient.PointReservationResult(
            9001L, 90L, "CONFIRMED", 44L, NOW.plusSeconds(300), true));

    assertThatThrownBy(() -> service.draw("SUMMER_LOTTERY", 7L, "request-1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("模拟确认积分时断网");

    var response = service.draw("SUMMER_LOTTERY", 7L, "request-1");

    assertThat(response.participationId()).isEqualTo(51L);
    assertThat(response.pointTransactionId()).isEqualTo(44L);
    verify(pointsClient, times(2))
        .reserve(9001L, 7L, 10L, "LOTTERY", "参与抽奖：SUMMER_LOTTERY");
    verify(participationStateService, times(2)).saveWaiting(7001L);
    verify(pointsClient, times(2)).confirmReservation(9001L);
    verify(participationStateService).complete(7001L, 44L);
  }

  @Test
  void retriesFinalTransactionAfterPointsWereConfirmed() {
    LotteryOrder order = order(PrizeType.VIRTUAL);
    LotteryParticipation participation = arrange(order);
    when(participationStateService.complete(7001L, 44L))
        .thenThrow(new IllegalStateException("模拟最终本地事务失败"))
        .thenAnswer(invocation -> {
          participation.markSuccess(44L, NOW);
          return new LotteryParticipationStateService.CompletionResult(participation, true);
        });

    assertThatThrownBy(() -> service.draw("SUMMER_LOTTERY", 7L, "request-1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("模拟最终本地事务失败");

    var response = service.draw("SUMMER_LOTTERY", 7L, "request-1");

    assertThat(response.participationId()).isEqualTo(51L);
    assertThat(response.pendingAwardCreated()).isTrue();
    verify(pointsClient, times(2)).confirmReservation(9001L);
    verify(participationStateService, times(2)).complete(7001L, 44L);
  }

  private LotteryParticipation arrange(LotteryOrder order) {
    when(orderCreationService.createOrGet("SUMMER_LOTTERY", 7L, "request-1"))
        .thenReturn(new LotteryOrderCreationService.CreationResult(order, false));
    when(pointsClient.reserve(9001L, 7L, 10L, "LOTTERY", "参与抽奖：SUMMER_LOTTERY"))
        .thenReturn(new PointsClient.PointReservationResult(
            9001L, 90L, "RESERVED", null, NOW.plusSeconds(300), false));
    when(pointsClient.confirmReservation(9001L))
        .thenReturn(new PointsClient.PointReservationResult(
            9001L, 90L, "CONFIRMED", 44L, NOW.plusSeconds(300), false));
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    when(participationStateService.saveWaiting(7001L)).thenReturn(participation);
    when(participationStateService.complete(7001L, 44L)).thenAnswer(invocation -> {
      participation.markSuccess(44L, NOW);
      return new LotteryParticipationStateService.CompletionResult(
          participation, order.getPrizeType() != PrizeType.NONE);
    });
    return participation;
  }

  private LotteryOrder order(PrizeType type) {
    IncentiveActivity activity = BeanUtils.instantiateClass(IncentiveActivity.class);
    ReflectionTestUtils.setField(activity, "id", 1L);
    ReflectionTestUtils.setField(activity, "code", "SUMMER_LOTTERY");
    ReflectionTestUtils.setField(activity, "name", "夏日抽奖");
    ReflectionTestUtils.setField(activity, "type", ActivityType.LOTTERY);
    ReflectionTestUtils.setField(activity, "status", ActivityStatus.ACTIVE);
    ParticipationRule rule = BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    ReflectionTestUtils.setField(rule, "pointsCost", 10L);
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", 31L);
    ReflectionTestUtils.setField(prize, "prizeId", 131L);
    ReflectionTestUtils.setField(prize, "prizeName", type == PrizeType.NONE ? "谢谢参与" : "优惠券");
    ReflectionTestUtils.setField(prize, "prizeType", type);
    return new LotteryOrder(7001L, "request-1", 7L, activity, rule, prize,
        9001L, "{\"passed\":true,\"usedTodayBefore\":0}", NOW);
  }
}
