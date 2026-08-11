package com.incentive.activity.repository;

import com.incentive.activity.domain.RedemptionRecord;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedemptionRecordRepository extends JpaRepository<RedemptionRecord, Long> {
  long countByActivityIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long activityId, Long userId, Instant from, Instant to);

  long countByItemId(Long itemId);
}
