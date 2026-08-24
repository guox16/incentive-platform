package com.incentive.activity.application;

import com.incentive.activity.domain.PendingAward;
import com.incentive.activity.infrastructure.AwardResultMessage;
import com.incentive.activity.repository.PendingAwardRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingAwardResultService {
  private final PendingAwardRepository repository;
  private final Clock clock;

  public PendingAwardResultService(PendingAwardRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public void apply(AwardResultMessage result) {
    PendingAward award = repository.findById(result.pendingAwardId())
        .orElseThrow(() -> new IllegalArgumentException(
            "待发奖记录不存在: " + result.pendingAwardId()));
    String expectedCommandKey = award.getSourceType() + ":" + award.getSourceRecordId();
    if (!expectedCommandKey.equals(result.commandKey())) {
      throw new IllegalArgumentException("发奖结果幂等号与待发奖记录不一致");
    }
    if (result.status() == AwardResultMessage.Status.AWARDED) {
      award.markAwarded(result.resultRef(), clock.instant());
    } else if (award.getStatus() == PendingAward.Status.PROCESSING) {
      award.markAwardFailed(result.errorMessage(), clock.instant());
    }
  }
}
