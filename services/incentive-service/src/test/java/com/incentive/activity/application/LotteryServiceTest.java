package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryParticipation;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.LotteryPrizePicker;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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
  @Mock private IncentiveActivityRepository activityRepository;
  @Mock private ActivityQueryService activityQueryService;
  @Mock private LotteryPrizeRepository prizeRepository;
  @Mock private LotteryParticipationRepository participationRepository;
  @Mock private PendingAwardRepository pendingAwardRepository;
  @Mock private LotteryPrizePicker prizePicker;
  @Mock private PointsClient pointsClient;
  @Mock private BusinessNumberGenerator businessNumberGenerator;
  private LotteryService service;

  @BeforeEach
  void setUp() {
    service = new LotteryService(activityRepository, activityQueryService, prizeRepository,
        participationRepository, pendingAwardRepository, prizePicker, pointsClient,
        businessNumberGenerator, Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
  }

  @Test
  void winningPrizeReservesAndConfirmsPointsBeforeCreatingPendingAward() {
    IncentiveActivity activity = activity();
    ParticipationRule rule = rule();
    LotteryPrize prize = prize(31L, PrizeType.VIRTUAL);
    arrange(activity, rule, prize);

    var response = service.draw("SUMMER_LOTTERY", 7L);

    assertThat(response.won()).isTrue();
    assertThat(response.pendingAwardCreated()).isTrue();
    assertThat(response.balanceAfter()).isEqualTo(90);
    verify(pointsClient).reserve(9001L, 7L, 10L, "LOTTERY", "参与抽奖：SUMMER_LOTTERY");
    verify(pointsClient).confirmReservation(9001L);
    verify(pendingAwardRepository).save(any(PendingAward.class));
  }

  @Test
  void nonePrizeOnlyPersistsParticipation() {
    IncentiveActivity activity = activity();
    ParticipationRule rule = rule();
    LotteryPrize prize = prize(32L, PrizeType.NONE);
    arrange(activity, rule, prize);

    var response = service.draw("SUMMER_LOTTERY", 7L);

    assertThat(response.won()).isFalse();
    assertThat(response.pendingAwardCreated()).isFalse();
    verify(pendingAwardRepository, never()).save(any());
  }

  private void arrange(IncentiveActivity activity, ParticipationRule rule, LotteryPrize prize) {
    when(activityRepository.findByCodeForUpdate("SUMMER_LOTTERY")).thenReturn(Optional.of(activity));
    when(activityQueryService.findRule(1L, NOW)).thenReturn(rule);
    when(participationRepository
        .countByActivityIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            any(), any(), any(), any())).thenReturn(0L);
    when(prizeRepository.findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(1L, 2L))
        .thenReturn(List.of(prize));
    when(prizePicker.pick(1L, 1, List.of(prize))).thenReturn(prize.getId());
    when(businessNumberGenerator.next()).thenReturn(9001L);
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

  private IncentiveActivity activity() {
    IncentiveActivity activity = BeanUtils.instantiateClass(IncentiveActivity.class);
    ReflectionTestUtils.setField(activity, "id", 1L);
    ReflectionTestUtils.setField(activity, "code", "SUMMER_LOTTERY");
    ReflectionTestUtils.setField(activity, "name", "夏日抽奖");
    ReflectionTestUtils.setField(activity, "type", ActivityType.LOTTERY);
    ReflectionTestUtils.setField(activity, "status", ActivityStatus.ACTIVE);
    ReflectionTestUtils.setField(activity, "startsAt", NOW.minusSeconds(3600));
    ReflectionTestUtils.setField(activity, "endsAt", NOW.plusSeconds(3600));
    return activity;
  }

  private ParticipationRule rule() {
    ParticipationRule rule = BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "activityId", 1L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    ReflectionTestUtils.setField(rule, "pointsCost", 10L);
    ReflectionTestUtils.setField(rule, "dailyLimit", 3);
    return rule;
  }

  private LotteryPrize prize(Long id, PrizeType type) {
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", id);
    ReflectionTestUtils.setField(prize, "activityId", 1L);
    ReflectionTestUtils.setField(prize, "ruleId", 2L);
    ReflectionTestUtils.setField(prize, "prizeId", id + 100);
    ReflectionTestUtils.setField(prize, "prizeName", type == PrizeType.NONE ? "谢谢参与" : "优惠券");
    ReflectionTestUtils.setField(prize, "prizeType", type);
    ReflectionTestUtils.setField(prize, "weight", 1L);
    return prize;
  }
}
