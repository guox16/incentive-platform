package com.incentive.activity.job;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.application.DailyCheckInService;
import com.incentive.activity.domain.RewardStatus;
import com.incentive.activity.repository.DailyCheckInRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class DailyCheckInRewardJobTest {
  @Mock private DailyCheckInRepository repository;
  @Mock private DailyCheckInService service;

  @Test
  void retriesPendingRecordsAndDefersTheFailureWithoutSkippingTheNextRecord() {
    Instant now = Instant.parse("2026-11-11T00:00:00Z");
    var job = new DailyCheckInRewardJob(repository, service, Clock.fixed(now, ZoneOffset.UTC));
    when(repository.findRewardRetryIds(RewardStatus.PENDING, now.minusSeconds(30),
        1, 2, PageRequest.of(0, 20))).thenReturn(List.of(11L, 13L));
    doThrow(new IllegalStateException("timeout")).when(service).retryReward(11L);

    job.executeShard(1, 2);

    verify(repository).recordRewardAttempt(11L, RewardStatus.PENDING, now);
    verify(service).retryReward(13L);
    verify(repository, never()).recordRewardAttempt(13L, RewardStatus.PENDING, now);
  }
}
