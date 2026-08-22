package com.incentive.activity.application;

import com.incentive.activity.infrastructure.AwardDispatchMessage;
import com.incentive.activity.infrastructure.AwardMessagePublisher;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PendingAwardDispatchService {
  private final PendingAwardDispatchStateService stateService;
  private final AwardMessagePublisher publisher;

  public PendingAwardDispatchService(
      PendingAwardDispatchStateService stateService, AwardMessagePublisher publisher) {
    this.stateService = stateService;
    this.publisher = publisher;
  }

  public DispatchResult dispatch(
      Long id, Instant failureBefore, Instant staleBefore, int maxRetries) {
    Optional<AwardDispatchMessage> claimed =
        stateService.claim(id, failureBefore, staleBefore, maxRetries);
    if (claimed.isEmpty()) return DispatchResult.SKIPPED;

    try {
      publisher.publish(claimed.get());
      return DispatchResult.PUBLISHED;
    } catch (RuntimeException failure) {
      try {
        stateService.markFailed(id, failure);
      } catch (RuntimeException stateFailure) {
        failure.addSuppressed(stateFailure);
        throw failure;
      }
      return DispatchResult.FAILED;
    }
  }

  public enum DispatchResult { PUBLISHED, SKIPPED, FAILED }
}
