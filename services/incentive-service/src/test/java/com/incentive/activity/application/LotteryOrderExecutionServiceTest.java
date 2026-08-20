package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class LotteryOrderExecutionServiceTest {
  @Mock private LotteryOrderProcessor processor;
  @Mock private LotteryRetryStateService retryStateService;

  @Test
  void synchronousFailureReturnsStableScheduledCode() {
    LotteryOrderExecutionService service =
        new LotteryOrderExecutionService(processor, retryStateService);
    IncentiveBusinessException failure = new IncentiveBusinessException(
        "POINTS_SERVICE_UNAVAILABLE", "积分服务暂不可用", HttpStatus.BAD_GATEWAY);
    when(processor.process(7001L)).thenThrow(failure);
    when(retryStateService.recordFailure(7001L, failure)).thenReturn(
        new LotteryRetryStateService.FailureRecord(
            false, false, true, "POINTS_SERVICE_UNAVAILABLE",
            Instant.parse("2026-08-20T00:30:05Z")));

    assertThatThrownBy(() -> service.execute(7001L))
        .isInstanceOfSatisfying(IncentiveBusinessException.class, ex -> {
          assertThat(ex.getCode()).isEqualTo("LOTTERY_RETRY_SCHEDULED");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
          assertThat(ex.getCause()).isSameAs(failure);
        });
  }

  @Test
  void automaticExecutionReportsRescheduledFailureWithoutThrowing() {
    LotteryOrderExecutionService service =
        new LotteryOrderExecutionService(processor, retryStateService);
    RuntimeException failure = new RuntimeException("temporary");
    when(processor.process(7001L)).thenThrow(failure);
    when(retryStateService.recordFailure(7001L, failure)).thenReturn(
        new LotteryRetryStateService.FailureRecord(
            false, false, true, "DATABASE_TEMPORARY_ERROR",
            Instant.parse("2026-08-20T00:30:05Z")));

    var result = service.executeAutomatically(7001L);

    assertThat(result.completed()).isFalse();
    assertThat(result.rescheduled()).isTrue();
    assertThat(result.terminal()).isFalse();
  }

  @Test
  void reloadsSuccessfulResultWhenFinalCommitResponseWasLost() {
    LotteryOrderExecutionService service =
        new LotteryOrderExecutionService(processor, retryStateService);
    RuntimeException lostResponse = new RuntimeException("最终事务结果未知");
    LotteryOrderProcessor.ProcessingResult completed =
        new LotteryOrderProcessor.ProcessingResult(null, null, null, false);
    when(processor.process(7001L)).thenThrow(lostResponse).thenReturn(completed);
    when(retryStateService.recordFailure(7001L, lostResponse)).thenReturn(
        new LotteryRetryStateService.FailureRecord(true, false, false, null, null));

    var result = service.execute(7001L);

    assertThat(result).isSameAs(completed);
  }
}
