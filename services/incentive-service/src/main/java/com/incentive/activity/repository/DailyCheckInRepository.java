package com.incentive.activity.repository;

import com.incentive.activity.domain.DailyCheckIn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {
  Optional<DailyCheckIn> findByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);
  Optional<DailyCheckIn> findTopByUserIdOrderByCheckInDateDesc(Long userId);
  List<DailyCheckIn> findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(
      Long userId, LocalDate startDate, LocalDate endDate);
}
