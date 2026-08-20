package com.incentive.activity.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.incentive.activity.application.LotteryOrderExecutionService;
import com.incentive.activity.repository.LotteryOrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LotteryOrderRetryJobTest {
  private static final Instant NOW = Instant.parse("2026-08-20T00:30:00Z");
  @Mock private LotteryOrderRepository orderRepository;
  @Mock private LotteryOrderExecutionService executionService;

  @Test
  void scansDueAndStaleOrdersAndContinuesEachOne() {
    LotteryOrderRetryJob job = new LotteryOrderRetryJob(
        orderRepository, executionService, Clock.fixed(NOW, ZoneOffset.UTC),
        Duration.ofSeconds(30), 100, 1000);
    when(orderRepository.findRecoverableOrderIds(
        eq(NOW), eq(NOW.minusSeconds(30)), eq(0), eq(1), any(Pageable.class)))
        .thenReturn(List.of(7001L, 7002L, 7003L));
    when(executionService.executeAutomatically(7001L)).thenReturn(
        new LotteryOrderExecutionService.AutomaticExecutionResult(true, false, false, null));
    when(executionService.executeAutomatically(7002L)).thenReturn(
        new LotteryOrderExecutionService.AutomaticExecutionResult(
            false, true, false, "POINTS_SERVICE_UNAVAILABLE"));
    when(executionService.executeAutomatically(7003L)).thenReturn(
        new LotteryOrderExecutionService.AutomaticExecutionResult(
            false, false, true, "POINT_RESERVATION_EXPIRED"));

    var result = job.executeShard(0, 1);

    assertThat(result.scanned()).isEqualTo(3);
    assertThat(result.completed()).isEqualTo(1);
    assertThat(result.rescheduled()).isEqualTo(1);
    assertThat(result.terminal()).isEqualTo(1);
  }
}
