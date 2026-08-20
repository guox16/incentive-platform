package com.incentive.activity.job;

import com.incentive.activity.application.LotteryOrderReconciliationService;
import com.incentive.activity.repository.LotteryOrderRepository;
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
public class LotteryOrderReconciliationJob {
  private final LotteryOrderRepository orderRepository;
  private final LotteryOrderReconciliationService reconciliationService;
  private final Clock clock;
  private final Duration staleThreshold;
  private final int batchSize;
  private final int maxPerRun;

  public LotteryOrderReconciliationJob(LotteryOrderRepository orderRepository,
      LotteryOrderReconciliationService reconciliationService, Clock clock,
      @Value("${lottery.retry.stale-threshold:PT30S}") Duration staleThreshold,
      @Value("${lottery.retry.batch-size:100}") int batchSize,
      @Value("${lottery.retry.max-per-run:1000}") int maxPerRun) {
    if (staleThreshold.isNegative() || staleThreshold.isZero()
        || batchSize <= 0 || maxPerRun <= 0) {
      throw new IllegalArgumentException("抽奖对账扫描参数不合法");
    }
    this.orderRepository = orderRepository;
    this.reconciliationService = reconciliationService;
    this.clock = clock;
    this.staleThreshold = staleThreshold;
    this.batchSize = batchSize;
    this.maxPerRun = maxPerRun;
  }

  @XxlJob("lotteryOrderReconciliationJob")
  public void execute() {
    int shardIndex = Math.max(XxlJobHelper.getShardIndex(), 0);
    int shardTotal = Math.max(XxlJobHelper.getShardTotal(), 1);
    ReconciliationSummary result = executeShard(shardIndex, shardTotal);
    XxlJobHelper.log(
        "抽奖单异常对账完成: shard={}/{}, scanned={}, completed={}, failed={}, deferred={}",
        shardIndex, shardTotal, result.scanned(), result.completed(),
        result.failed(), result.deferred());
  }

  ReconciliationSummary executeShard(int shardIndex, int shardTotal) {
    if (shardTotal <= 0 || shardIndex < 0 || shardIndex >= shardTotal) {
      throw new IllegalArgumentException("XXL-JOB分片参数不合法");
    }
    int scanned = 0;
    int completed = 0;
    int failed = 0;
    int deferred = 0;

    while (scanned < maxPerRun) {
      int limit = Math.min(batchSize, maxPerRun - scanned);
      Instant now = clock.instant();
      List<Long> orderIds = orderRepository.findReconciliationOrderIds(
          now, now.minus(staleThreshold), shardIndex, shardTotal, PageRequest.of(0, limit));
      if (orderIds.isEmpty()) break;

      for (Long orderId : orderIds) {
        LotteryOrderReconciliationService.ReconciliationResult result =
            reconciliationService.reconcile(orderId);
        if (result == LotteryOrderReconciliationService.ReconciliationResult.COMPLETED) completed++;
        else if (result == LotteryOrderReconciliationService.ReconciliationResult.FAILED) failed++;
        else deferred++;
      }
      scanned += orderIds.size();
      if (orderIds.size() < limit) break;
    }
    return new ReconciliationSummary(scanned, completed, failed, deferred);
  }

  record ReconciliationSummary(int scanned, int completed, int failed, int deferred) {}
}

