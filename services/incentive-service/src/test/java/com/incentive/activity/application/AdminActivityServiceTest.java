package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.application.lottery.LotteryPreDrawRuleChain;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleDefinition;
import com.incentive.activity.application.lottery.LotteryPreDrawRuleStore;
import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.dto.CreateActivityRequest;
import com.incentive.activity.dto.LotteryPreDrawRuleRequest;
import com.incentive.activity.dto.UpdateActivityRequest;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.ParticipationRuleRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminActivityServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-12T08:00:00Z");
  @Mock IncentiveActivityRepository activityRepository;
  @Mock ParticipationRuleRepository ruleRepository;
  @Mock LotteryPreDrawRuleStore preDrawRuleStore;
  @Mock LotteryPrizeRepository lotteryPrizeRepository;
  @Mock LotteryPreDrawRuleChain preDrawRuleChain;
  @Mock BusinessNumberGenerator businessNumberGenerator;
  AdminActivityService service;

  @BeforeEach
  void setUp() {
    service = new AdminActivityService(activityRepository, ruleRepository, preDrawRuleStore,
        lotteryPrizeRepository, preDrawRuleChain, businessNumberGenerator,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsDraftActivityWithFirstRule() {
    when(businessNumberGenerator.next()).thenReturn(12345L);
    when(activityRepository.save(any(IncentiveActivity.class))).thenAnswer(call -> {
      IncentiveActivity activity = call.getArgument(0);
      setId(activity, 9L);
      return activity;
    });
    when(ruleRepository.saveAndFlush(any(ParticipationRule.class))).thenAnswer(call -> {
      ParticipationRule rule = call.getArgument(0);
      setId(rule, 19L);
      return rule;
    });
    when(ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(9L))
        .thenReturn(Optional.of(rule(19L, 9L, 1, 20, 3, NOW)));

    var response = service.create(new CreateActivityRequest("夏日抽奖",
        ActivityType.LOTTERY, NOW, NOW.plusSeconds(3600), 20, 3, null, List.of()));

    assertThat(response.code()).isEqualTo("LOTTERY_9IX");
    assertThat(response.status()).isEqualTo(ActivityStatus.DRAFT);
    assertThat(response.ruleVersion()).isEqualTo(1);
    verify(preDrawRuleChain).validateConfiguration(List.of(), null);
  }

  @Test
  void redemptionActivityDoesNotPersistDailyLimit() {
    when(businessNumberGenerator.next()).thenReturn(12346L);
    when(activityRepository.save(any(IncentiveActivity.class))).thenAnswer(call -> {
      IncentiveActivity activity = call.getArgument(0);
      setId(activity, 10L);
      return activity;
    });
    when(ruleRepository.saveAndFlush(any(ParticipationRule.class))).thenAnswer(call -> {
      ParticipationRule rule = call.getArgument(0);
      setId(rule, 20L);
      return rule;
    });
    when(ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(10L))
        .thenReturn(Optional.of(rule(20L, 10L, 1, 0, null, NOW)));

    service.create(new CreateActivityRequest("积分商城",
        ActivityType.REDEMPTION, NOW, null, 0, 3, null, List.of()));

    ArgumentCaptor<ParticipationRule> captor = ArgumentCaptor.forClass(ParticipationRule.class);
    verify(ruleRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getDailyLimit()).isNull();
  }

  @Test
  void createsNewRuleVersionWhenRuleChanges() {
    IncentiveActivity activity = activity(7L);
    ParticipationRule oldRule = rule(17L, 7L, 2, 10, 1, NOW.minusSeconds(60));
    when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
    when(ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(7L))
        .thenReturn(Optional.of(oldRule));
    when(ruleRepository.saveAndFlush(any(ParticipationRule.class))).thenAnswer(call -> {
      ParticipationRule rule = call.getArgument(0);
      setId(rule, 18L);
      return rule;
    });

    service.update(7L, new UpdateActivityRequest("更新后的抽奖", ActivityStatus.ACTIVE,
        NOW, NOW.plusSeconds(7200), 30, 2, null, List.of()));

    ArgumentCaptor<ParticipationRule> captor = ArgumentCaptor.forClass(ParticipationRule.class);
    verify(ruleRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getRuleVersion()).isEqualTo(3);
    assertThat(captor.getValue().getPointsCost()).isEqualTo(30);
  }

  @Test
  void storesEachLotteryRuleAsAnIndependentVersionedRow() {
    when(businessNumberGenerator.next()).thenReturn(12347L);
    when(activityRepository.save(any(IncentiveActivity.class))).thenAnswer(call -> {
      IncentiveActivity activity = call.getArgument(0);
      setId(activity, 11L);
      return activity;
    });
    when(ruleRepository.saveAndFlush(any(ParticipationRule.class))).thenAnswer(call -> {
      ParticipationRule rule = call.getArgument(0);
      setId(rule, 21L);
      return rule;
    });
    when(ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(11L))
        .thenReturn(Optional.of(rule(21L, 11L, 1, 10, 3, NOW)));
    service.create(new CreateActivityRequest("规则抽奖", ActivityType.LOTTERY,
        NOW, NOW.plusSeconds(3600), 10, 3, 999L,
        List.of(new LotteryPreDrawRuleRequest(
            "USER_LIST", true, List.of(7L, 8L), null, null))));

    verify(preDrawRuleStore).save(org.mockito.ArgumentMatchers.eq(11L),
        org.mockito.ArgumentMatchers.eq(21L),
        org.mockito.ArgumentMatchers.argThat(definitions -> definitions.size() == 2
            && definitions.get(0).parameters()
                instanceof LotteryPreDrawRuleDefinition.UserListParameters users
            && users.userIds().containsAll(List.of(7L, 8L))
            && definitions.get(1).parameters()
                instanceof LotteryPreDrawRuleDefinition.LuckyFallbackParameters lucky
            && lucky.prizeId().equals(999L)));
  }

  @Test
  void rejectsInvalidActivityTime() {
    assertThatThrownBy(() -> service.create(new CreateActivityRequest("错误时间",
        ActivityType.REDEMPTION, NOW, NOW, 0, null, null, List.of())))
        .isInstanceOf(IncentiveBusinessException.class)
        .hasMessage("结束时间必须晚于开始时间");
  }

  @Test
  void rejectsCheckInManagementThroughGenericRules() {
    assertThatThrownBy(() -> service.create(new CreateActivityRequest("签到",
        ActivityType.CHECK_IN, NOW, null, 0, null, null, List.of())))
        .isInstanceOf(IncentiveBusinessException.class)
        .hasMessage("签到活动请使用签到规则管理");
  }

  private IncentiveActivity activity(Long id) {
    IncentiveActivity activity = new IncentiveActivity("DRAW", ActivityType.LOTTERY, "抽奖",
        NOW, NOW.plusSeconds(3600));
    setId(activity, id);
    return activity;
  }

  private void setId(IncentiveActivity activity, Long id) {
    try {
      Field field = IncentiveActivity.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(activity, id);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }

  private ParticipationRule rule(Long id, Long activityId, int version, long pointsCost,
      Integer dailyLimit, Instant effectiveFrom) {
    ParticipationRule rule = new ParticipationRule(
        activityId, version, pointsCost, dailyLimit, effectiveFrom);
    setId(rule, id);
    return rule;
  }

  private void setId(ParticipationRule rule, Long id) {
    try {
      Field field = ParticipationRule.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(rule, id);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }
}
