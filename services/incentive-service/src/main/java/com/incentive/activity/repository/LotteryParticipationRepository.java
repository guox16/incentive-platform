package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryParticipation;
import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotteryParticipationRepository extends JpaRepository<LotteryParticipation, Long> {
  Optional<LotteryParticipation> findByLotteryOrderId(Long lotteryOrderId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select participation from LotteryParticipation participation "
      + "where participation.lotteryOrderId = :lotteryOrderId")
  Optional<LotteryParticipation> findByLotteryOrderIdForUpdate(
      @Param("lotteryOrderId") Long lotteryOrderId);

  long countByActivityIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long activityId, Long userId, Instant from, Instant to);

  long countByActivityIdAndUserId(Long activityId, Long userId);
}
