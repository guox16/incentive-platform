package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.LotteryRecordStatus;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryOrderRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryRecordQueryServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-20T00:30:00Z");
  @Mock private LotteryOrderRepository orderRepository;
  @Mock private IncentiveActivityRepository activityRepository;

  @Test
  void hidesFixedPrizeUntilOrderSucceeds() {
    LotteryOrder processing = order(7001L, "request-1");
    LotteryOrder successful = order(7002L, "request-2");
    successful.markPointsReserved(NOW.plusSeconds(300), 90L, NOW);
    successful.markResultSaved(NOW);
    successful.markSuccess(NOW);
    when(orderRepository.findByUserIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
        .thenReturn(List.of(processing, successful));
    when(activityRepository.findAllById(List.of(1L))).thenReturn(List.of(activity()));

    var records = new LotteryRecordQueryService(orderRepository, activityRepository)
        .findRecentByUser(7L);

    assertThat(records.get(0).status()).isEqualTo(LotteryRecordStatus.PROCESSING);
    assertThat(records.get(0).prizeId()).isNull();
    assertThat(records.get(1).status()).isEqualTo(LotteryRecordStatus.SUCCESS);
    assertThat(records.get(1).prizeName()).isEqualTo("优惠券");
  }

  private LotteryOrder order(Long id, String requestId) {
    ParticipationRule rule = BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    ReflectionTestUtils.setField(rule, "pointsCost", 10L);
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", 31L);
    ReflectionTestUtils.setField(prize, "prizeId", 131L);
    ReflectionTestUtils.setField(prize, "prizeName", "优惠券");
    ReflectionTestUtils.setField(prize, "prizeType", PrizeType.VIRTUAL);
    return new LotteryOrder(id, requestId, 7L, activity(), rule, prize,
        9000L + id, "{\"passed\":true}", NOW);
  }

  private IncentiveActivity activity() {
    IncentiveActivity activity = BeanUtils.instantiateClass(IncentiveActivity.class);
    ReflectionTestUtils.setField(activity, "id", 1L);
    ReflectionTestUtils.setField(activity, "code", "SUMMER_LOTTERY");
    ReflectionTestUtils.setField(activity, "name", "夏日抽奖");
    ReflectionTestUtils.setField(activity, "type", ActivityType.LOTTERY);
    ReflectionTestUtils.setField(activity, "status", ActivityStatus.ACTIVE);
    return activity;
  }
}
