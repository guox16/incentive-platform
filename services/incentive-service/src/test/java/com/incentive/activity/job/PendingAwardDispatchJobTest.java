package com.incentive.activity.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.incentive.activity.application.PendingAwardDispatchService;
import com.incentive.activity.repository.PendingAwardRepository;
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
class PendingAwardDispatchJobTest {
  @Mock private PendingAwardRepository repository;
  @Mock private PendingAwardDispatchService dispatchService;

  @Test
  void scansAndDispatchesCandidateAwards() {
    Instant now = Instant.parse("2026-08-22T00:00:00Z");
    when(repository.findDispatchCandidateIds(
        any(), any(), eq(8), eq(0), eq(1), any(Pageable.class)))
        .thenReturn(List.of(1L, 2L));
    when(dispatchService.dispatch(eq(1L), any(), any(), eq(8)))
        .thenReturn(PendingAwardDispatchService.DispatchResult.PUBLISHED);
    when(dispatchService.dispatch(eq(2L), any(), any(), eq(8)))
        .thenReturn(PendingAwardDispatchService.DispatchResult.FAILED);
    PendingAwardDispatchJob job = new PendingAwardDispatchJob(
        repository, dispatchService, Clock.fixed(now, ZoneOffset.UTC),
        Duration.ofSeconds(30), Duration.ofSeconds(30), 8, 100, 1000);

    PendingAwardDispatchJob.DispatchSummary result = job.executeShard(0, 1);

    assertThat(result.scanned()).isEqualTo(2);
    assertThat(result.published()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
  }
}
