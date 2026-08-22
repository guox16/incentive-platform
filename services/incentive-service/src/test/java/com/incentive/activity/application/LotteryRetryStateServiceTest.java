package com.incentive.activity.application;

import com.incentive.activity.application.lottery.LotteryPostDrawStockRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.repository.LotteryOrderRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryRetryStateServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-20T00:30:00Z");
  @Mock private LotteryOrderRepository orderRepository;
  @Mock private LotteryPostDrawStockRule postDrawStockRule;
  private LotteryRetryStateService service;

  @BeforeEach
  void setUp() {
    LotteryRetryPolicy policy = new LotteryRetryPolicy();
    service = new LotteryRetryStateService(
        orderRepository, policy, postDrawStockRule,
        Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(5));
  }

  @Test
  void keepsCheckpointAndSchedulesTransientFailure() {
    LotteryOrder order = order();
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));

    var record = service.recordFailure(7001L, failure("POINTS_SERVICE_UNAVAILABLE"));

    assertThat(record.retryScheduled()).isTrue();
    assertThat(order.getStatus()).isEqualTo(LotteryOrderStatus.INIT);
    assertThat(order.getFailureCode()).isEqualTo("POINTS_SERVICE_UNAVAILABLE");
    assertThat(order.getRetryCount()).isEqualTo(1);
    assertThat(order.getNextRetryAt()).isEqualTo(NOW.plusSeconds(5));
  }

  @Test
  void marksPermanentFailureTerminal() {
    LotteryOrder order = order();
    ReflectionTestUtils.setField(order, "stockNo", 9L);
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));

    var record = service.recordFailure(7001L, failure("INSUFFICIENT_POINTS"));

    assertThat(record.terminal()).isTrue();
    assertThat(order.getStatus()).isEqualTo(LotteryOrderStatus.FAILED);
    assertThat(order.getFailureCode()).isEqualTo("INSUFFICIENT_POINTS");
    assertThat(order.getStockNo()).isNull();
    assertThat(order.getNextRetryAt()).isNull();
    verify(postDrawStockRule).release(7001L, 1L, 131L, 9L);
  }

  @Test
  void advancedOrderIsReconciledBeforePermanentFailure() {
    LotteryOrder order = order();
    order.markPointsReserved(NOW.plusSeconds(300), 90L, NOW);
    when(orderRepository.findByIdForUpdate(7001L)).thenReturn(Optional.of(order));

    var record = service.recordFailure(7001L, failure("POINT_RESERVATION_EXPIRED"));

    assertThat(record.retryScheduled()).isTrue();
    assertThat(order.getStatus()).isEqualTo(LotteryOrderStatus.POINTS_RESERVED);
    assertThat(order.getNextRetryAt()).isEqualTo(NOW.plusSeconds(5));
  }

  private IncentiveBusinessException failure(String code) {
    return new IncentiveBusinessException(code, "测试失败", HttpStatus.BAD_GATEWAY);
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
