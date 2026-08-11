package com.incentive.activity.application;

import com.incentive.activity.domain.DailyCheckIn;
import com.incentive.activity.domain.RewardStatus;
import com.incentive.activity.dto.DailyCheckInResponse;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.DailyCheckInRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DailyCheckInService {
  private static final long DAILY_REWARD_POINTS = 10;
  private final DailyCheckInRepository repository;
  private final PointsClient pointsClient;
  private final Clock clock;

  public DailyCheckInService(DailyCheckInRepository repository, PointsClient pointsClient, Clock clock) {
    this.repository = repository;
    this.pointsClient = pointsClient;
    this.clock = clock;
  }

  public DailyCheckInResponse getStatus(Long userId) {
    LocalDate today = LocalDate.now(clock);
    DailyCheckIn todayRecord = repository.findByUserIdAndCheckInDate(userId, today).orElse(null);
    int currentStreak = todayRecord != null
        ? todayRecord.getStreakDays()
        : repository.findTopByUserIdOrderByCheckInDateDesc(userId)
            .filter(latest -> latest.getCheckInDate().equals(today.minusDays(1)))
            .map(DailyCheckIn::getStreakDays)
            .orElse(0);
    return response(userId, today, todayRecord, currentStreak, null);
  }

  @Transactional
  public DailyCheckInResponse checkIn(Long userId) {
    LocalDate today = LocalDate.now(clock);
    DailyCheckIn record = repository.findByUserIdAndCheckInDate(userId, today).orElse(null);
    if (record == null) {
      int streakDays = repository.findTopByUserIdOrderByCheckInDateDesc(userId)
          .filter(latest -> latest.getCheckInDate().equals(today.minusDays(1)))
          .map(latest -> latest.getStreakDays() + 1)
          .orElse(1);
      record = repository.saveAndFlush(new DailyCheckIn(
          userId, today, streakDays, DAILY_REWARD_POINTS, clock.instant()));
    }

    Long balanceAfter = null;
    if (record.getRewardStatus() == RewardStatus.PENDING) {
      var credit = pointsClient.credit(record.getId(), userId, record.getRewardPoints());
      record.markAwarded(credit.transactionId(), clock.instant());
      balanceAfter = credit.balanceAfter();
    }
    return response(userId, today, record, record.getStreakDays(), balanceAfter);
  }

  private DailyCheckInResponse response(Long userId, LocalDate today, DailyCheckIn record,
      int currentStreak, Long balanceAfter) {
    YearMonth month = YearMonth.from(today);
    List<LocalDate> signedDates = repository
        .findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(userId, month.atDay(1), month.atEndOfMonth())
        .stream().map(DailyCheckIn::getCheckInDate).toList();
    return new DailyCheckInResponse(userId, today, record != null, currentStreak,
        record == null ? DAILY_REWARD_POINTS : record.getRewardPoints(),
        record == null ? "AVAILABLE" : record.getRewardStatus().name(),
        record == null ? null : record.getId(),
        record == null ? null : record.getPointTransactionId(),
        balanceAfter, signedDates);
  }
}
