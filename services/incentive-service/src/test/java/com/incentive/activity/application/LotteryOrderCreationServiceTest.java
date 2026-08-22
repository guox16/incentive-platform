package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleChain;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleStore;
import com.incentive.activity.domain.LotteryPoolEntry;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.LotteryPrizePicker;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.LotteryParticipationRepository;
import com.incentive.activity.support.IncentiveBusinessException;
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
class LotteryOrderCreationServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");
  @Mock private IncentiveActivityRepository activityRepository;
  @Mock private ActivityQueryService activityQueryService;
  @Mock private LotteryPrizeRepository prizeRepository;
  @Mock private LotteryOrderRepository orderRepository;
  @Mock private LotteryParticipationRepository participationRepository;
  @Mock private LotteryPreDrawRuleStore preDrawRuleStore;
  @Mock private LotteryPrizePicker prizePicker;
  @Mock private LotteryPreDrawRuleChain preDrawRuleChain;
  @Mock private BusinessNumberGenerator businessNumberGenerator;
  private LotteryOrderCreationService service;

  @BeforeEach
  void setUp() {
    service = new LotteryOrderCreationService(activityRepository, activityQueryService,
        prizeRepository, orderRepository, participationRepository, preDrawRuleStore,
        prizePicker, preDrawRuleChain, businessNumberGenerator,
        Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
  }

  @Test
  void createsInitOrderWithFixedResultAndTwoBusinessIds() {
    IncentiveActivity activity = activity();
    ParticipationRule rule = rule();
    LotteryPrize prize = prize();
    arrangeNewOrder(activity, rule, prize);
    when(businessNumberGenerator.next()).thenReturn(7001L, 9001L);
    when(orderRepository.saveAndFlush(any(LotteryOrder.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.createOrGet("SUMMER_LOTTERY", 7L, " request-1 ");

    assertThat(result.replayed()).isFalse();
    assertThat(result.order().getId()).isEqualTo(7001L);
    assertThat(result.order().getPointsBusinessId()).isEqualTo(9001L);
    assertThat(result.order().getRequestId()).isEqualTo("request-1");
    assertThat(result.order().getLotteryPrizeId()).isEqualTo(31L);
    assertThat(result.order().getStatus()).isEqualTo(LotteryOrderStatus.INIT);
    assertThat(result.order().getFailureCode()).isNull();
    assertThat(result.order().getRetryCount()).isZero();
    assertThat(result.order().getNextRetryAt()).isNull();
    verify(preDrawRuleStore).load(2L);
    verify(businessNumberGenerator, org.mockito.Mockito.times(2)).next();
  }

  @Test
  void sameRequestReturnsExistingOrderWithoutDrawingAgain() {
    IncentiveActivity activity = activity();
    LotteryOrder existing = new LotteryOrder(7001L, "request-1", 7L, activity, rule(), prize(),
        9001L, "{\"passed\":true,\"usedTodayBefore\":0}", NOW);
    when(activityRepository.findByCodeForUpdate("SUMMER_LOTTERY"))
        .thenReturn(Optional.of(activity));
    when(orderRepository.findByUserIdAndActivityIdAndRequestId(7L, 1L, "request-1"))
        .thenReturn(Optional.of(existing));

    var result = service.createOrGet("SUMMER_LOTTERY", 7L, "request-1");

    assertThat(result.replayed()).isTrue();
    assertThat(result.order()).isSameAs(existing);
    verify(prizePicker, never()).pick(any(), any(Integer.class), any());
    verify(businessNumberGenerator, never()).next();
  }

  @Test
  void rejectsNewOrderWhenDailyLimitIsReached() {
    IncentiveActivity activity = activity();
    ParticipationRule rule = rule();
    when(activityRepository.findByCodeForUpdate("SUMMER_LOTTERY"))
        .thenReturn(Optional.of(activity));
    when(orderRepository.findByUserIdAndActivityIdAndRequestId(7L, 1L, "request-1"))
        .thenReturn(Optional.empty());
    when(activityQueryService.findRule(1L, NOW)).thenReturn(rule);
    when(orderRepository
        .countByActivityIdAndUserIdAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            any(), any(), any(), any(), any())).thenReturn(3L);

    assertThatThrownBy(() -> service.createOrGet("SUMMER_LOTTERY", 7L, "request-1"))
        .isInstanceOfSatisfying(IncentiveBusinessException.class,
            error -> assertThat(error.getCode()).isEqualTo("DAILY_LIMIT_REACHED"));
    verify(businessNumberGenerator, never()).next();
  }

  private void arrangeNewOrder(
      IncentiveActivity activity, ParticipationRule rule, LotteryPrize prize) {
    when(activityRepository.findByCodeForUpdate("SUMMER_LOTTERY"))
        .thenReturn(Optional.of(activity));
    when(orderRepository.findByUserIdAndActivityIdAndRequestId(7L, 1L, "request-1"))
        .thenReturn(Optional.empty());
    when(activityQueryService.findRule(1L, NOW)).thenReturn(rule);
    when(orderRepository
        .countByActivityIdAndUserIdAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            any(), any(), any(), any(), any())).thenReturn(0L);
    when(prizeRepository.findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(1L, 2L))
        .thenReturn(List.of(prize));
    var pool = List.of(LotteryPoolEntry.original(prize));
    when(preDrawRuleChain.resolve(any(), any(), any()))
        .thenReturn(new LotteryPreDrawRuleChain.Resolution(pool, null, null));
    when(prizePicker.pick(1L, 1, pool)).thenReturn(31L);
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

  private LotteryPrize prize() {
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", 31L);
    ReflectionTestUtils.setField(prize, "activityId", 1L);
    ReflectionTestUtils.setField(prize, "ruleId", 2L);
    ReflectionTestUtils.setField(prize, "prizeId", 131L);
    ReflectionTestUtils.setField(prize, "prizeName", "优惠券");
    ReflectionTestUtils.setField(prize, "prizeType", PrizeType.VIRTUAL);
    ReflectionTestUtils.setField(prize, "weight", 1L);
    return prize;
  }
}
