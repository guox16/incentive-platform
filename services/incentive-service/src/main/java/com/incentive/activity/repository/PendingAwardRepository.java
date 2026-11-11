package com.incentive.activity.repository;

import com.incentive.activity.domain.PendingAward;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingAwardRepository extends JpaRepository<PendingAward, Long> {
  @Query("""
      select participation.lotteryOrderId as orderId, award.status as status,
        award.retryCount as retryCount
      from LotteryParticipation participation
      join PendingAward award on award.sourceRecordId = participation.id
      where participation.userId = :userId and award.userId = :userId
        and award.sourceType = :sourceType and participation.lotteryOrderId in :orderIds
      """)
  List<LotteryAwardProgress> findLotteryAwardProgress(@Param("userId") Long userId,
      @Param("sourceType") PendingAward.SourceType sourceType,
      @Param("orderIds") Collection<Long> orderIds);

  interface LotteryAwardProgress {
    Long getOrderId();
    PendingAward.Status getStatus();
    int getRetryCount();
  }

  @Query(value = "select id from pending_awards where "
      + "(status = 'PENDING' "
      + "or (status = 'FAILED' and retry_count < :maxRetries and updated_at <= :failureBefore) "
      + "or (status = 'PROCESSING' and updated_at <= :staleBefore)) "
      + "and mod(id, :shardTotal) = :shardIndex order by id", nativeQuery = true)
  List<Long> findDispatchCandidateIds(
      @Param("failureBefore") Instant failureBefore,
      @Param("staleBefore") Instant staleBefore,
      @Param("maxRetries") int maxRetries,
      @Param("shardIndex") int shardIndex,
      @Param("shardTotal") int shardTotal,
      Pageable pageable);

  @Modifying
  @Query(value = "update pending_awards set status = 'PROCESSING', last_error = null, "
      + "updated_at = :now where id = :id and "
      + "(status = 'PENDING' "
      + "or (status = 'FAILED' and retry_count < :maxRetries and updated_at <= :failureBefore) "
      + "or (status = 'PROCESSING' and updated_at <= :staleBefore))", nativeQuery = true)
  int claimForDispatch(
      @Param("id") Long id,
      @Param("now") Instant now,
      @Param("failureBefore") Instant failureBefore,
      @Param("staleBefore") Instant staleBefore,
      @Param("maxRetries") int maxRetries);

  @Modifying
  @Query(value = "update pending_awards set status = 'FAILED', retry_count = retry_count + 1, "
      + "last_error = :error, updated_at = :now "
      + "where id = :id and status = 'PROCESSING'", nativeQuery = true)
  int markDispatchFailed(
      @Param("id") Long id, @Param("error") String error, @Param("now") Instant now);
}
