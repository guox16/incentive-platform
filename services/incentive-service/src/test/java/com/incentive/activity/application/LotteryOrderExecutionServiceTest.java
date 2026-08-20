package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
  void immediatelyRetriesOnceAndReturnsResult() {
    LotteryOrderExecutionService service =
        new LotteryOrderExecutionService(processor, retryStateService);
    RuntimeException firstFailure = new RuntimeException("temporary");
    LotteryOrderProcessor.ProcessingResult completed =
        new LotteryOrderProcessor.ProcessingResult(null, null, null, false);
    when(processor.process(7001L)).thenThrow(firstFailure).thenReturn(completed);

    assertThat(service.execute(7001L)).isSameAs(completed);
    verify(processor, times(2)).process(7001L);
    verify(retryStateService, never()).recordFailure(7001L, firstFailure);
  }

  @Test
  void secondFailureSchedulesReconciliation() {
    LotteryOrderExecutionService service =
        new LotteryOrderExecutionService(processor, retryStateService);
    RuntimeException firstFailure = new RuntimeException("first");
    IncentiveBusinessException secondFailure = new IncentiveBusinessException(
        "POINTS_SERVICE_UNAVAILABLE", "积分服务暂不可用", HttpStatus.BAD_GATEWAY);
    when(processor.process(7001L)).thenThrow(firstFailure, secondFailure);
    when(retryStateService.recordFailure(7001L, secondFailure)).thenReturn(
        new LotteryRetryStateService.FailureRecord(
            false, false, true, "POINTS_SERVICE_UNAVAILABLE",
            Instant.parse("2026-08-20T00:30:05Z")));

    assertThatThrownBy(() -> service.execute(7001L))
        .isInstanceOfSatisfying(IncentiveBusinessException.class, ex -> {
          assertThat(ex.getCode()).isEqualTo("LOTTERY_RETRY_SCHEDULED");
          assertThat(ex.getCause()).isSameAs(secondFailure);
        });
  }

  @Test
  void reloadsSuccessfulResultWhenFinalCommitResponseWasLostTwice() {
    LotteryOrderExecutionService service =
        new LotteryOrderExecutionService(processor, retryStateService);
    RuntimeException firstFailure = new RuntimeException("first response lost");
    RuntimeException secondFailure = new RuntimeException("second response lost");
    LotteryOrderProcessor.ProcessingResult completed =
        new LotteryOrderProcessor.ProcessingResult(null, null, null, false);
    when(processor.process(7001L))
        .thenThrow(firstFailure, secondFailure)
        .thenReturn(completed);
    when(retryStateService.recordFailure(7001L, secondFailure)).thenReturn(
        new LotteryRetryStateService.FailureRecord(true, false, false, null, null));

    assertThat(service.execute(7001L)).isSameAs(completed);
  }
}
