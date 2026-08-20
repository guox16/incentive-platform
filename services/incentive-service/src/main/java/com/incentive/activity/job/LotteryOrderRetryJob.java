package com.incentive.activity.job;

import com.incentive.activity.application.LotteryOrderExecutionService;
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
public class LotteryOrderRetryJob {
  private final LotteryOrderRepository orderRepository;
  private final LotteryOrderExecutionService executionService;
  private final Clock clock;
  private final Duration staleThreshold;
  private final int batchSize;
  private final int maxPerRun;

  public LotteryOrderRetryJob(LotteryOrderRepository orderRepository,
      LotteryOrderExecutionService executionService, Clock clock,
      @Value("${lottery.retry.stale-threshold:PT30S}") Duration staleThreshold,
      @Value("${lottery.retry.batch-size:100}") int batchSize,
      @Value("${lottery.retry.max-per-run:1000}") int maxPerRun) {
    if (staleThreshold.isNegative() || staleThreshold.isZero()
        || batchSize <= 0 || maxPerRun <= 0) {
      throw new IllegalArgumentException("抽奖重试扫描参数不合法");
    }
    this.orderRepository = orderRepository;
    this.executionService = executionService;
    this.clock = clock;
    this.staleThreshold = staleThreshold;
    this.batchSize = batchSize;
    this.maxPerRun = maxPerRun;
  }

  @XxlJob("lotteryOrderRetryJob")
  public void execute() {
    int shardIndex = Math.max(XxlJobHelper.getShardIndex(), 0);
    int shardTotal = Math.max(XxlJobHelper.getShardTotal(), 1);
    RetryResult result = executeShard(shardIndex, shardTotal);
    XxlJobHelper.log(
        "抽奖单自动重试完成: shard={}/{}, scanned={}, completed={}, rescheduled={}, terminal={}",
        shardIndex, shardTotal, result.scanned(), result.completed(),
        result.rescheduled(), result.terminal());
  }

  RetryResult executeShard(int shardIndex, int shardTotal) {
    if (shardTotal <= 0 || shardIndex < 0 || shardIndex >= shardTotal) {
      throw new IllegalArgumentException("XXL-JOB分片参数不合法");
    }
    int scanned = 0;
    int completed = 0;
    int rescheduled = 0;
    int terminal = 0;

    while (scanned < maxPerRun) {
      int limit = Math.min(batchSize, maxPerRun - scanned);
      Instant now = clock.instant();
      List<Long> orderIds = orderRepository.findRecoverableOrderIds(
          now, now.minus(staleThreshold), shardIndex, shardTotal, PageRequest.of(0, limit));
      if (orderIds.isEmpty()) break;

      for (Long orderId : orderIds) {
        LotteryOrderExecutionService.AutomaticExecutionResult result =
            executionService.executeAutomatically(orderId);
        if (result.completed()) completed++;
        else if (result.rescheduled()) rescheduled++;
        else if (result.terminal()) terminal++;
      }
      scanned += orderIds.size();
      if (orderIds.size() < limit) break;
    }
    return new RetryResult(scanned, completed, rescheduled, terminal);
  }

  record RetryResult(int scanned, int completed, int rescheduled, int terminal) {}
}
