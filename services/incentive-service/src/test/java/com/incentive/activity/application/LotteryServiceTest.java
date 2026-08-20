package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-20T00:30:00Z");
  @Mock private LotteryOrderCreationService orderCreationService;
  @Mock private LotteryOrderExecutionService executionService;
  private LotteryService service;

  @BeforeEach
  void setUp() {
    service = new LotteryService(orderCreationService, executionService);
  }

  @Test
  void returnsResultOnlyAfterOrderExecutionCompletes() {
    LotteryOrder order = order();
    LotteryParticipation participation = new LotteryParticipation(order, NOW);
    ReflectionTestUtils.setField(participation, "id", 51L);
    participation.markSuccess(44L, NOW);
    PointsClient.PointReservationResult pointsResult = new PointsClient.PointReservationResult(
        9001L, 90L, "CONFIRMED", 44L, NOW.plusSeconds(300), true);
    when(orderCreationService.createOrGet("SUMMER_LOTTERY", 7L, "request-1"))
        .thenReturn(new LotteryOrderCreationService.CreationResult(order, false));
    when(executionService.execute(7001L)).thenReturn(
        new LotteryOrderProcessor.ProcessingResult(order, participation, pointsResult, true));

    var response = service.draw("SUMMER_LOTTERY", 7L, "request-1");

    assertThat(response.participationId()).isEqualTo(51L);
    assertThat(response.prizeId()).isEqualTo(131L);
    assertThat(response.pointTransactionId()).isEqualTo(44L);
    assertThat(response.balanceAfter()).isEqualTo(90L);
    assertThat(response.pendingAwardCreated()).isTrue();
    verify(executionService).execute(7001L);
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
