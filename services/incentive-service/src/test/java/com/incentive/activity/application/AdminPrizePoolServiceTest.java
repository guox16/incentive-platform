package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.UpdatePrizePoolRequest;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.ParticipationRuleRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPrizePoolServiceTest {
  private static final String AUTHORIZATION = "Bearer admin-token";
  @Mock IncentiveActivityRepository activityRepository;
  @Mock ParticipationRuleRepository ruleRepository;
  @Mock LotteryPrizeRepository prizeRepository;
  @Mock AwardCatalog awardCatalog;
  AdminPrizePoolService service;

  @BeforeEach
  void setUp() {
    service = new AdminPrizePoolService(
        activityRepository, ruleRepository, prizeRepository, awardCatalog);
  }

  @Test
  void candidatesOnlyContainUsableUnassignedAwards() {
    prepareDraft();
    when(prizeRepository.findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(7L, 17L))
        .thenReturn(List.of());
    when(prizeRepository.findPrizeIdsAssignedToOtherLatestPools(7L)).thenReturn(Set.of(102L));
    when(awardCatalog.list(AUTHORIZATION)).thenReturn(List.of(
        award(101L, PrizeType.VIRTUAL, "ACTIVE", 10),
        award(102L, PrizeType.POINTS, "ACTIVE", 10),
        award(103L, PrizeType.VIRTUAL, "INACTIVE", 10),
        award(104L, PrizeType.VIRTUAL, "ACTIVE", 0),
        award(105L, PrizeType.NONE, "ACTIVE", 0)));

    var response = service.get(7L, AUTHORIZATION);

    assertThat(response.candidates()).extracting(item -> item.prizeId())
        .containsExactly(101L, 105L);
  }

  @Test
  void replacesDraftPoolWithServerSnapshots() {
    prepareDraft();
    when(prizeRepository.findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(7L, 17L))
        .thenReturn(List.of());
    when(prizeRepository.findPrizeIdsAssignedToOtherLatestPools(7L)).thenReturn(Set.of());
    when(awardCatalog.list(AUTHORIZATION)).thenReturn(List.of(
        award(101L, PrizeType.VIRTUAL, "ACTIVE", 10)));

    service.update(7L, new UpdatePrizePoolRequest(
        List.of(new UpdatePrizePoolRequest.Entry(101L, 25, 8L))), AUTHORIZATION);

    ArgumentCaptor<List<com.incentive.activity.domain.LotteryPrize>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(prizeRepository).saveAll(captor.capture());
    var saved = captor.getValue().get(0);
    assertThat(saved.getActivityId()).isEqualTo(7L);
    assertThat(saved.getRuleId()).isEqualTo(17L);
    assertThat(saved.getPrizeName()).isEqualTo("奖品101");
    assertThat(saved.getWeight()).isEqualTo(25);
    assertThat(saved.getCampaignQuota()).isEqualTo(8L);
  }

  @Test
  void rejectsAwardAssignedToAnotherCurrentPool() {
    prepareDraft();
    when(prizeRepository.findPrizeIdsAssignedToOtherLatestPools(7L)).thenReturn(Set.of(101L));
    when(awardCatalog.list(AUTHORIZATION)).thenReturn(List.of(
        award(101L, PrizeType.VIRTUAL, "ACTIVE", 10)));

    assertThatThrownBy(() -> service.update(7L, new UpdatePrizePoolRequest(
        List.of(new UpdatePrizePoolRequest.Entry(101L, 1, 5L))), AUTHORIZATION))
        .isInstanceOf(IncentiveBusinessException.class)
        .hasMessage("所选奖品已参与其他活动，请重新选择");
  }

  private void prepareDraft() {
    IncentiveActivity activity = new IncentiveActivity(
        "LOTTERY_7", ActivityType.LOTTERY, "抽奖", Instant.now(), null);
    setId(activity, 7L);
    ParticipationRule rule = new ParticipationRule(7L, 1, 10, 3, Instant.now());
    setId(rule, 17L);
    when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
    when(ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(7L))
        .thenReturn(Optional.of(rule));
  }

  private AwardCatalog.Item award(Long id, PrizeType type, String status, long stock) {
    return new AwardCatalog.Item(id, "PRIZE_" + id, "奖品" + id, type, status,
        null, null, stock, stock);
  }

  private void setId(Object target, Long id) {
    try {
      Field field = target.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(target, id);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError(ex);
    }
  }
}
