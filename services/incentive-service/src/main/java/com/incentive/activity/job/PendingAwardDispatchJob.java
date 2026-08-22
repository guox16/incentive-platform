package com.incentive.activity.job;

import com.incentive.activity.application.PendingAwardDispatchService;
import com.incentive.activity.repository.PendingAwardRepository;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class PendingAwardDispatchJob {
  private final PendingAwardRepository repository;
  private final PendingAwardDispatchService dispatchService;
  private final Clock clock;
  private final Duration staleThreshold;
  private final Duration failureBackoff;
  private final int maxRetries;
  private final int batchSize;
  private final int maxPerRun;

  public PendingAwardDispatchJob(
      PendingAwardRepository repository,
      PendingAwardDispatchService dispatchService,
      Clock clock,
      @Value("${award.dispatch.stale-threshold:PT30S}") Duration staleThreshold,
      @Value("${award.dispatch.failure-backoff:PT30S}") Duration failureBackoff,
      @Value("${award.dispatch.max-retries:8}") int maxRetries,
      @Value("${award.dispatch.batch-size:100}") int batchSize,
      @Value("${award.dispatch.max-per-run:1000}") int maxPerRun) {
    if (staleThreshold.isNegative() || staleThreshold.isZero()
        || failureBackoff.isNegative() || failureBackoff.isZero()
        || maxRetries <= 0 || batchSize <= 0 || maxPerRun <= 0) {
      throw new IllegalArgumentException("待发奖扫描参数不合法");
    }
    this.repository = repository;
    this.dispatchService = dispatchService;
    this.clock = clock;
    this.staleThreshold = staleThreshold;
    this.failureBackoff = failureBackoff;
    this.maxRetries = maxRetries;
    this.batchSize = batchSize;
    this.maxPerRun = maxPerRun;
  }

  @XxlJob("pendingAwardDispatchJob")
  public void execute() {
    int shardIndex = Math.max(XxlJobHelper.getShardIndex(), 0);
    int shardTotal = Math.max(XxlJobHelper.getShardTotal(), 1);
    DispatchSummary summary = executeShard(shardIndex, shardTotal);
    XxlJobHelper.log(
        "待发奖消息投递完成: shard={}/{}, scanned={}, published={}, skipped={}, failed={}",
        shardIndex, shardTotal, summary.scanned(), summary.published(),
        summary.skipped(), summary.failed());
    if (summary.failed() > 0) {
      throw new IllegalStateException("存在 " + summary.failed() + " 条待发奖消息投递失败");
    }
  }

  DispatchSummary executeShard(int shardIndex, int shardTotal) {
    if (shardTotal <= 0 || shardIndex < 0 || shardIndex >= shardTotal) {
      throw new IllegalArgumentException("XXL-JOB分片参数不合法");
    }
    int scanned = 0;
    int published = 0;
    int skipped = 0;
    int failed = 0;

    while (scanned < maxPerRun) {
      int limit = Math.min(batchSize, maxPerRun - scanned);
      Instant now = clock.instant();
      Instant failureBefore = now.minus(failureBackoff);
      Instant staleBefore = now.minus(staleThreshold);
      List<Long> ids = repository.findDispatchCandidateIds(
          failureBefore, staleBefore, maxRetries, shardIndex, shardTotal,
          PageRequest.of(0, limit));
      if (ids.isEmpty()) break;

      for (Long id : ids) {
        try {
          PendingAwardDispatchService.DispatchResult result = dispatchService.dispatch(
              id, failureBefore, staleBefore, maxRetries);
          if (result == PendingAwardDispatchService.DispatchResult.PUBLISHED) published++;
          else if (result == PendingAwardDispatchService.DispatchResult.SKIPPED) skipped++;
          else failed++;
        } catch (RuntimeException ex) {
          failed++;
          XxlJobHelper.log("待发奖消息投递异常: pendingAwardId={}, error={}", id, ex.getMessage());
        }
      }
      scanned += ids.size();
      if (ids.size() < limit) break;
    }
    return new DispatchSummary(scanned, published, skipped, failed);
  }

  record DispatchSummary(int scanned, int published, int skipped, int failed) {}
}
