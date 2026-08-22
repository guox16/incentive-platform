package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.infrastructure.AwardDispatchMessage;
import com.incentive.activity.infrastructure.AwardMessagePublisher;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingAwardDispatchServiceTest {
  private static final Instant FAILURE_BEFORE = Instant.parse("2026-08-22T00:00:00Z");
  private static final Instant STALE_BEFORE = Instant.parse("2026-08-21T23:59:30Z");

  @Mock private PendingAwardDispatchStateService stateService;
  @Mock private AwardMessagePublisher publisher;
  @InjectMocks private PendingAwardDispatchService service;

  @Test
  void publishesClaimedAward() {
    AwardDispatchMessage message = message();
    when(stateService.claim(1L, FAILURE_BEFORE, STALE_BEFORE, 8))
        .thenReturn(Optional.of(message));

    var result = service.dispatch(1L, FAILURE_BEFORE, STALE_BEFORE, 8);

    assertThat(result).isEqualTo(PendingAwardDispatchService.DispatchResult.PUBLISHED);
    verify(publisher).publish(message);
  }

  @Test
  void recordsPublisherFailure() {
    AwardDispatchMessage message = message();
    RuntimeException failure = new RuntimeException("broker unavailable");
    when(stateService.claim(1L, FAILURE_BEFORE, STALE_BEFORE, 8))
        .thenReturn(Optional.of(message));
    doThrow(failure).when(publisher).publish(message);

    var result = service.dispatch(1L, FAILURE_BEFORE, STALE_BEFORE, 8);

    assertThat(result).isEqualTo(PendingAwardDispatchService.DispatchResult.FAILED);
    verify(stateService).markFailed(1L, failure);
  }

  @Test
  void skipsAwardClaimedByAnotherExecutor() {
    when(stateService.claim(1L, FAILURE_BEFORE, STALE_BEFORE, 8))
        .thenReturn(Optional.empty());

    assertThat(service.dispatch(1L, FAILURE_BEFORE, STALE_BEFORE, 8))
        .isEqualTo(PendingAwardDispatchService.DispatchResult.SKIPPED);
  }

  private AwardDispatchMessage message() {
    return new AwardDispatchMessage(
        1L, "LOTTERY:11", PendingAward.SourceType.LOTTERY, 11L,
        7L, 101L, "100积分", "POINTS", "{\"points\":100}", 3L);
  }
}
