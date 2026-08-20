package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotteryOrderRepository extends JpaRepository<LotteryOrder, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select lotteryOrder from LotteryOrder lotteryOrder where lotteryOrder.id = :id")
  Optional<LotteryOrder> findByIdForUpdate(@Param("id") Long id);

  Optional<LotteryOrder> findByUserIdAndActivityIdAndRequestId(
      Long userId, Long activityId, String requestId);

  @Query("select lotteryOrder.id from LotteryOrder lotteryOrder "
      + "where lotteryOrder.status <> com.incentive.activity.domain.LotteryOrderStatus.SUCCESS "
      + "and lotteryOrder.status <> com.incentive.activity.domain.LotteryOrderStatus.FAILED "
      + "and ((lotteryOrder.nextRetryAt is not null and lotteryOrder.nextRetryAt <= :now) "
      + "or (lotteryOrder.nextRetryAt is null and lotteryOrder.updatedAt <= :staleBefore)) "
      + "and mod(lotteryOrder.id, :shardTotal) = :shardIndex "
      + "order by lotteryOrder.updatedAt, lotteryOrder.id")
  List<Long> findReconciliationOrderIds(@Param("now") Instant now,
      @Param("staleBefore") Instant staleBefore,
      @Param("shardIndex") int shardIndex, @Param("shardTotal") int shardTotal,
      Pageable pageable);

  long countByActivityIdAndUserIdAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long activityId, Long userId, LotteryOrderStatus status, Instant from, Instant to);
}
