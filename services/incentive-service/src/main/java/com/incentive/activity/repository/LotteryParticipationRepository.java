package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryParticipation;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryParticipationRepository extends JpaRepository<LotteryParticipation, Long> {
  long countByActivityIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long activityId, Long userId, Instant from, Instant to);
}
