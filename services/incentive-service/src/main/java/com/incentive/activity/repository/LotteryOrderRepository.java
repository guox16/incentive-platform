package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
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

  long countByActivityIdAndUserIdAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long activityId, Long userId, LotteryOrderStatus status, Instant from, Instant to);
}
