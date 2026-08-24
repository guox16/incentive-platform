package com.incentive.activity.repository;

import com.incentive.activity.domain.RedemptionRecord;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RedemptionRecordRepository extends JpaRepository<RedemptionRecord, Long> {
  Optional<RedemptionRecord> findByRequestId(String requestId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select record from RedemptionRecord record where record.id = :id")
  Optional<RedemptionRecord> findByIdForUpdate(@Param("id") Long id);

  long countByItemId(Long itemId);

  long countByActivityId(Long activityId);
}
