package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.domain.RedemptionItem;
import com.incentive.activity.domain.RedemptionRecord;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.PendingAwardRepository;
import com.incentive.activity.repository.RedemptionItemRepository;
import com.incentive.activity.repository.RedemptionRecordRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedemptionServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");
  @Mock private IncentiveActivityRepository activityRepository;
  @Mock private ActivityQueryService activityQueryService;
  @Mock private RedemptionItemRepository itemRepository;
  @Mock private RedemptionRecordRepository recordRepository;
  @Mock private PendingAwardRepository pendingAwardRepository;
  @Mock private PointsClient pointsClient;
  @Mock private BusinessNumberGenerator businessNumberGenerator;
  private RedemptionService service;

  @BeforeEach
  void setUp() {
    service = new RedemptionService(activityRepository, activityQueryService, itemRepository,
        recordRepository, pendingAwardRepository, pointsClient, businessNumberGenerator,
        Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));
  }

  @Test
  void redeemDebitsItemPriceAndCreatesPendingAward() {
    IncentiveActivity activity = activity();
    ParticipationRule rule = rule();
    RedemptionItem item = item(10L);
    arrangeBase(activity, rule, item);
    when(recordRepository.countByItemId(10L)).thenReturn(0L);
    when(businessNumberGenerator.next()).thenReturn(8001L);
    when(pointsClient.debit(8001L, 7L, 50L, "REDEMPTION", "兑换商品：WELCOME_COUPON"))
        .thenReturn(new PointsClient.PointDebitResult(61L, 150L));
    when(recordRepository.saveAndFlush(any(RedemptionRecord.class))).thenAnswer(invocation -> {
      RedemptionRecord record = invocation.getArgument(0);
      ReflectionTestUtils.setField(record, "id", 71L);
      return record;
    });

    var response = service.redeem("POINTS_MALL", 10L, 7L);

    assertThat(response.redemptionId()).isEqualTo(71L);
    assertThat(response.pointsCost()).isEqualTo(50L);
    assertThat(response.balanceAfter()).isEqualTo(150L);
    verify(pendingAwardRepository).save(any(PendingAward.class));
  }

  @Test
  void soldOutCampaignQuotaDoesNotDebitPoints() {
    IncentiveActivity activity = activity();
    ParticipationRule rule = rule();
    RedemptionItem item = item(10L);
    arrangeBase(activity, rule, item);
    when(recordRepository.countByItemId(10L)).thenReturn(10L);

    assertThatThrownBy(() -> service.redeem("POINTS_MALL", 10L, 7L))
        .isInstanceOf(IncentiveBusinessException.class)
        .hasMessage("兑换商品活动名额已用完");
    verify(pointsClient, never()).debit(any(), any(), anyLong(), any(), any());
  }

  private void arrangeBase(IncentiveActivity activity, ParticipationRule rule, RedemptionItem item) {
    when(activityRepository.findByCodeForUpdate("POINTS_MALL")).thenReturn(Optional.of(activity));
    when(activityQueryService.findRule(1L, NOW)).thenReturn(rule);
    when(recordRepository
        .countByActivityIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            any(), any(), any(), any())).thenReturn(0L);
    when(itemRepository.findByIdAndActivityIdAndRuleIdAndStatus(
        10L, 1L, 2L, RedemptionItem.Status.ACTIVE)).thenReturn(Optional.of(item));
  }

  private IncentiveActivity activity() {
    IncentiveActivity activity = BeanUtils.instantiateClass(IncentiveActivity.class);
    ReflectionTestUtils.setField(activity, "id", 1L);
    ReflectionTestUtils.setField(activity, "code", "POINTS_MALL");
    ReflectionTestUtils.setField(activity, "name", "积分商城");
    ReflectionTestUtils.setField(activity, "type", ActivityType.REDEMPTION);
    ReflectionTestUtils.setField(activity, "status", ActivityStatus.ACTIVE);
    ReflectionTestUtils.setField(activity, "startsAt", NOW.minusSeconds(3600));
    ReflectionTestUtils.setField(activity, "endsAt", NOW.plusSeconds(3600));
    return activity;
  }

  private ParticipationRule rule() {
    ParticipationRule rule = BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    ReflectionTestUtils.setField(rule, "dailyLimit", 5);
    return rule;
  }

  private RedemptionItem item(Long id) {
    RedemptionItem item = BeanUtils.instantiateClass(RedemptionItem.class);
    ReflectionTestUtils.setField(item, "id", id);
    ReflectionTestUtils.setField(item, "activityId", 1L);
    ReflectionTestUtils.setField(item, "ruleId", 2L);
    ReflectionTestUtils.setField(item, "itemCode", "WELCOME_COUPON");
    ReflectionTestUtils.setField(item, "prizeId", 101L);
    ReflectionTestUtils.setField(item, "prizeName", "新人优惠券");
    ReflectionTestUtils.setField(item, "prizeType", PrizeType.VIRTUAL);
    ReflectionTestUtils.setField(item, "pointsPrice", 50L);
    ReflectionTestUtils.setField(item, "campaignQuota", 10L);
    return item;
  }
}
