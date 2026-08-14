package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.dto.CreateActivityRequest;
import com.incentive.activity.dto.UpdateActivityRequest;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.ParticipationRuleRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
  AdminActivityService service;

  @BeforeEach
  void setUp() {
    service = new AdminActivityService(activityRepository, ruleRepository, new ObjectMapper(),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsDraftActivityWithFirstRule() {
    when(activityRepository.existsByCode("SUMMER_DRAW")).thenReturn(false);
    when(activityRepository.save(any(IncentiveActivity.class))).thenAnswer(call -> {
      IncentiveActivity activity = call.getArgument(0);
      setId(activity, 9L);
      return activity;
    });
    when(ruleRepository.save(any(ParticipationRule.class))).thenAnswer(call -> call.getArgument(0));
    when(ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(9L))
        .thenReturn(Optional.of(new ParticipationRule(9L, 1, 20, 3, null, NOW)));

    var response = service.create(new CreateActivityRequest("SUMMER_DRAW", "夏日抽奖",
        ActivityType.LOTTERY, NOW, NOW.plusSeconds(3600), 20, 3, null));

    assertThat(response.status()).isEqualTo(ActivityStatus.DRAFT);
    assertThat(response.ruleVersion()).isEqualTo(1);
  }

  @Test
  void createsNewRuleVersionWhenRuleChanges() {
    IncentiveActivity activity = activity(7L);
    ParticipationRule oldRule = new ParticipationRule(7L, 2, 10, 1, null, NOW.minusSeconds(60));
    when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
    when(ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(7L))
        .thenReturn(Optional.of(oldRule));

    service.update(7L, new UpdateActivityRequest("更新后的抽奖", ActivityStatus.ACTIVE,
        NOW, NOW.plusSeconds(7200), 30, 2, "{\"level\":2}"));

    ArgumentCaptor<ParticipationRule> captor = ArgumentCaptor.forClass(ParticipationRule.class);
    verify(ruleRepository).save(captor.capture());
    assertThat(captor.getValue().getRuleVersion()).isEqualTo(3);
    assertThat(captor.getValue().getPointsCost()).isEqualTo(30);
  }

  @Test
  void rejectsInvalidActivityTime() {
    assertThatThrownBy(() -> service.create(new CreateActivityRequest("BAD_TIME", "错误时间",
        ActivityType.REDEMPTION, NOW, NOW, 0, null, null)))
        .isInstanceOf(IncentiveBusinessException.class)
        .hasMessage("结束时间必须晚于开始时间");
  }

  @Test
  void rejectsCheckInManagementThroughGenericRules() {
    assertThatThrownBy(() -> service.create(new CreateActivityRequest("CHECK_IN", "签到",
        ActivityType.CHECK_IN, NOW, null, 0, null, null)))
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
}
