package com.incentive.points.job;

import com.incentive.points.repository.PointReservationRepository;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** XXL-JOB 过期积分预占扫描任务。 */
@Component
public class PointReservationCompensationJob {
  private final PointReservationRepository reservationRepository;
  private final PointReservationCompensationExecutor compensationExecutor;
  private final int batchSize;
  private final int maxPerRun;

  public PointReservationCompensationJob(PointReservationRepository reservationRepository,
      PointReservationCompensationExecutor compensationExecutor,
      @Value("${points.reservation.compensation.batch-size:100}") int batchSize,
      @Value("${points.reservation.compensation.max-per-run:1000}") int maxPerRun) {
    if (batchSize <= 0 || maxPerRun <= 0) {
      throw new IllegalArgumentException("积分预占补偿批次参数必须大于0");
    }
    this.reservationRepository = reservationRepository;
    this.compensationExecutor = compensationExecutor;
    this.batchSize = batchSize;
    this.maxPerRun = maxPerRun;
  }

  /** 按 XXL-JOB 分片广播参数扫描并补偿过期预占。 */
  @XxlJob("pointReservationCompensationJob")
  public void execute() {
    int shardIndex = Math.max(XxlJobHelper.getShardIndex(), 0);
    int shardTotal = Math.max(XxlJobHelper.getShardTotal(), 1);
    CompensationResult result = executeShard(shardIndex, shardTotal);
    XxlJobHelper.log(
        "积分预占补偿完成: shard={}/{}, scanned={}, refunded={}, skipped={}, failed={}",
        shardIndex, shardTotal, result.scanned(), result.refunded(), result.skipped(), result.failed());
    if (result.failed() > 0) {
      throw new IllegalStateException("存在 " + result.failed() + " 条积分预占补偿失败");
    }
  }

  CompensationResult executeShard(int shardIndex, int shardTotal) {
    if (shardTotal <= 0 || shardIndex < 0 || shardIndex >= shardTotal) {
      throw new IllegalArgumentException("XXL-JOB 分片参数不合法");
    }
    int scanned = 0;
    int refunded = 0;
    int skipped = 0;
    int failed = 0;

    while (scanned < maxPerRun) {
      int limit = Math.min(batchSize, maxPerRun - scanned);
      List<Long> businessIds = reservationRepository.findExpiredBusinessIdsForShard(
          shardIndex, shardTotal, PageRequest.of(0, limit));
      if (businessIds.isEmpty()) break;

      for (Long businessId : businessIds) {
        try {
          if (compensationExecutor.expireAndRefund(businessId)) refunded++;
          else skipped++;
        } catch (RuntimeException ex) {
          failed++;
          XxlJobHelper.log("积分预占补偿失败: businessId={}, error={}", businessId, ex.getMessage());
        }
      }
      scanned += businessIds.size();
      if (businessIds.size() < limit) break;
    }
    return new CompensationResult(scanned, refunded, skipped, failed);
  }

  record CompensationResult(int scanned, int refunded, int skipped, int failed) {}
}
