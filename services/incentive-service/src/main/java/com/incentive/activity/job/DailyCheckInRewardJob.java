package com.incentive.activity.job;

import com.incentive.activity.application.DailyCheckInService;
import com.incentive.activity.domain.RewardStatus;
import com.incentive.activity.repository.DailyCheckInRepository;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class DailyCheckInRewardJob {
  private static final PageRequest BATCH = PageRequest.of(0, 20);
  private final DailyCheckInRepository repository;
  private final DailyCheckInService service;
  private final Clock clock;

  public DailyCheckInRewardJob(DailyCheckInRepository repository, DailyCheckInService service,
      Clock clock) {
    this.repository = repository;
    this.service = service;
    this.clock = clock;
  }

  @XxlJob("dailyCheckInRewardJob")
  public void execute() {
    executeShard(Math.max(0, XxlJobHelper.getShardIndex()), Math.max(1, XxlJobHelper.getShardTotal()));
  }

  void executeShard(int shardIndex, int shardTotal) {
    var checkInIds = repository.findRewardRetryIds(RewardStatus.PENDING,
        clock.instant().minusSeconds(30), shardIndex, shardTotal, BATCH);
    int deferred = 0;
    for (Long checkInId : checkInIds) {
      try {
        service.retryReward(checkInId);
      } catch (RuntimeException failure) {
        deferred++;
        XxlJobHelper.log("签到积分待补发: checkInId={}, error={}", checkInId, failure.getMessage());
        repository.recordRewardAttempt(checkInId, RewardStatus.PENDING, clock.instant());
      }
    }
    XxlJobHelper.log("签到积分补发扫描: shard={}/{}, scanned={}, deferred={}",
        shardIndex, shardTotal, checkInIds.size(), deferred);
  }
}
