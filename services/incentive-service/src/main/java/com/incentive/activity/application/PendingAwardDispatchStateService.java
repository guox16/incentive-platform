package com.incentive.activity.application;

import com.incentive.activity.infrastructure.AwardDispatchMessage;
import com.incentive.activity.repository.PendingAwardRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingAwardDispatchStateService {
  private final PendingAwardRepository repository;
  private final Clock clock;

  public PendingAwardDispatchStateService(PendingAwardRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public Optional<AwardDispatchMessage> claim(
      Long id, Instant failureBefore, Instant staleBefore, int maxRetries) {
    Instant now = clock.instant();
    if (repository.claimForDispatch(
        id, now, failureBefore, staleBefore, maxRetries) != 1) {
      return Optional.empty();
    }
    return repository.findById(id).map(AwardDispatchMessage::from);
  }

  @Transactional
  public void markFailed(Long id, RuntimeException failure) {
    String message = failure.getMessage();
    if (message == null || message.isBlank()) message = failure.getClass().getSimpleName();
    if (message.length() > 500) message = message.substring(0, 500);
    repository.markDispatchFailed(id, message, clock.instant());
  }
}
