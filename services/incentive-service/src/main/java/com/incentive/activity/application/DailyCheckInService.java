package com.incentive.activity.application;

import com.incentive.activity.domain.DailyCheckIn;
import com.incentive.activity.domain.RewardStatus;
import com.incentive.activity.dto.DailyCheckInResponse;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.DailyCheckInRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DailyCheckInService {
  private static final long DAILY_REWARD_POINTS = 10;
  private final DailyCheckInRepository repository;
  private final DailyCheckInTransactions transactions;
  private final PointsClient pointsClient;
  private final Clock clock;

  public DailyCheckInService(DailyCheckInRepository repository,
      DailyCheckInTransactions transactions, PointsClient pointsClient, Clock clock) {
    this.repository = repository;
    this.transactions = transactions;
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

  public DailyCheckInResponse checkIn(Long userId, String requestId) {
    LocalDate today = LocalDate.now(clock);
    DailyCheckIn record = findExisting(userId, requestId, today);
    if (record == null) {
      int streakDays = repository.findTopByUserIdOrderByCheckInDateDesc(userId)
          .filter(latest -> latest.getCheckInDate().equals(today.minusDays(1)))
          .map(latest -> latest.getStreakDays() + 1)
          .orElse(1);
      try {
        record = transactions.create(
            requestId, userId, today, streakDays, DAILY_REWARD_POINTS, clock.instant());
      } catch (DataIntegrityViolationException ex) {
        record = findExisting(userId, requestId, today);
        if (record == null) throw ex;
      }
    }

    Long balanceAfter = null;
    if (record.getRewardStatus() == RewardStatus.PENDING) {
      var credit = pointsClient.credit(record.getId(), userId, record.getRewardPoints());
      record = transactions.markAwarded(record.getId(), credit.transactionId(), clock.instant());
      balanceAfter = credit.balanceAfter();
    }
    return response(userId, today, record, record.getStreakDays(), balanceAfter);
  }

  private DailyCheckIn findExisting(Long userId, String requestId, LocalDate businessDate) {
    DailyCheckIn byRequest = repository.findByRequestId(requestId).orElse(null);
    if (byRequest != null) {
      if (!byRequest.getUserId().equals(userId)) {
        throw new IncentiveBusinessException(
            "CHECK_IN_REQUEST_ID_REUSED", "签到请求号已被其他用户使用", HttpStatus.CONFLICT);
      }
      return byRequest;
    }
    return repository.findByUserIdAndCheckInDate(userId, businessDate).orElse(null);
  }

  private DailyCheckInResponse response(Long userId, LocalDate today, DailyCheckIn record,
      int currentStreak, Long balanceAfter) {
    YearMonth month = YearMonth.from(today);
    List<LocalDate> signedDates = repository
        .findByUserIdAndCheckInDateBetweenOrderByCheckInDateAsc(userId, month.atDay(1), month.atEndOfMonth())
        .stream().map(DailyCheckIn::getCheckInDate).toList();
    boolean checkedInToday = record != null && record.getCheckInDate().equals(today);
    return new DailyCheckInResponse(userId, today, checkedInToday, currentStreak,
        record == null ? DAILY_REWARD_POINTS : record.getRewardPoints(),
        record == null ? "AVAILABLE" : record.getRewardStatus().name(),
        record == null ? null : record.getId(),
        record == null ? null : record.getPointTransactionId(),
        balanceAfter, signedDates);
  }
}
