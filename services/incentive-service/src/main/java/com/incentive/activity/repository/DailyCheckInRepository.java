package com.incentive.activity.repository;

import com.incentive.activity.domain.DailyCheckIn;
import com.incentive.activity.domain.RewardStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {
  Optional<DailyCheckIn> findByRequestId(String requestId);
  Optional<DailyCheckIn> findByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);
  Optional<DailyCheckIn> findTopByUserIdOrderByCheckInDateDesc(Long userId);
  List<DailyCheckIn> findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(
      Long userId, LocalDate startDate, LocalDate endDate);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select checkIn from DailyCheckIn checkIn where checkIn.id = :id")
  Optional<DailyCheckIn> findByIdForUpdate(@Param("id") Long id);

  @Query("""
      select checkIn.id from DailyCheckIn checkIn
      where checkIn.rewardStatus = :status and checkIn.updatedAt <= :before
        and mod(checkIn.id, :shardTotal) = :shardIndex
      order by checkIn.updatedAt, checkIn.id
      """)
  List<Long> findRewardRetryIds(@Param("status") RewardStatus status, @Param("before") Instant before,
      @Param("shardIndex") int shardIndex, @Param("shardTotal") int shardTotal, Pageable page);

  @Transactional
  @Modifying
  @Query("""
      update DailyCheckIn checkIn set checkIn.updatedAt = :attemptedAt
      where checkIn.id = :id and checkIn.rewardStatus = :status and checkIn.updatedAt < :attemptedAt
      """)
  void recordRewardAttempt(@Param("id") Long id, @Param("status") RewardStatus status,
      @Param("attemptedAt") Instant attemptedAt);
}
