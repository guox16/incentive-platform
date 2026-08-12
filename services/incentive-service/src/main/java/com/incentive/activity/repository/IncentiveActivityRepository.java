package com.incentive.activity.repository;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncentiveActivityRepository extends JpaRepository<IncentiveActivity, Long> {
  @Query("""
      select activity from IncentiveActivity activity
      where activity.status = :status
        and activity.type in :types
        and activity.startsAt <= :now
        and (activity.endsAt is null or activity.endsAt > :now)
      order by activity.startsAt desc, activity.id desc
      """)
  List<IncentiveActivity> findActive(
      @Param("status") ActivityStatus status,
      @Param("types") List<ActivityType> types,
      @Param("now") Instant now);

  Optional<IncentiveActivity> findByCode(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select activity from IncentiveActivity activity where activity.code = :code")
  Optional<IncentiveActivity> findByCodeForUpdate(@Param("code") String code);
}
