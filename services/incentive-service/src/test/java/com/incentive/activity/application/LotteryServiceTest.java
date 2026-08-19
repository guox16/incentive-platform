package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");
  @Mock private LotteryOrderCreationService orderCreationService;
  @Mock private LotteryParticipationRepository participationRepository;
  @Mock private PendingAwardRepository pendingAwardRepository;
  @Mock private PointsClient pointsClient;
  private LotteryService service;

  @BeforeEach
  void setUp() {
    service = new LotteryService(orderCreationService, participationRepository,
        pendingAwardRepository, pointsClient, Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
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
    verify(pointsClient).confirmReservation(9001L);
    verify(pendingAwardRepository).save(any(PendingAward.class));
  }

  @Test
  void nonePrizeOnlyPersistsParticipation() {
    LotteryOrder order = order(PrizeType.NONE);
    arrange(order);

    var response = service.draw("SUMMER_LOTTERY", 7L, "request-1");

    assertThat(response.won()).isFalse();
    assertThat(response.pendingAwardCreated()).isFalse();
    verify(pendingAwardRepository, never()).save(any());
  }

  private void arrange(LotteryOrder order) {
    when(orderCreationService.createOrGet("SUMMER_LOTTERY", 7L, "request-1"))
        .thenReturn(new LotteryOrderCreationService.CreationResult(order, false));
    when(pointsClient.reserve(9001L, 7L, 10L, "LOTTERY", "参与抽奖：SUMMER_LOTTERY"))
        .thenReturn(new PointsClient.PointReservationResult(
            9001L, 90L, "RESERVED", null, NOW.plusSeconds(300), false));
    when(pointsClient.confirmReservation(9001L))
        .thenReturn(new PointsClient.PointReservationResult(
            9001L, 90L, "CONFIRMED", 44L, NOW.plusSeconds(300), false));
    when(participationRepository.saveAndFlush(any(LotteryParticipation.class)))
        .thenAnswer(invocation -> {
          LotteryParticipation participation = invocation.getArgument(0);
          ReflectionTestUtils.setField(participation, "id", 51L);
          return participation;
        });
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
