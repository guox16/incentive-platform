package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryOrderStatus;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryOrderRepository extends JpaRepository<LotteryOrder, Long> {
  Optional<LotteryOrder> findByUserIdAndActivityIdAndRequestId(
      Long userId, Long activityId, String requestId);

  long countByActivityIdAndUserIdAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long activityId, Long userId, LotteryOrderStatus status, Instant from, Instant to);
}
