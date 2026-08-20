package com.incentive.activity.application;

import com.incentive.activity.domain.DailyCheckIn;
import com.incentive.activity.repository.DailyCheckInRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 签到写事务边界；远程积分调用不得进入这些本地数据库事务。 */
@Service
public class DailyCheckInTransactions {
  private final DailyCheckInRepository repository;

  public DailyCheckInTransactions(DailyCheckInRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public DailyCheckIn create(String requestId, Long userId, LocalDate businessDate,
      int streakDays, long rewardPoints, Instant now) {
    return repository.saveAndFlush(new DailyCheckIn(
        requestId, userId, businessDate, streakDays, rewardPoints, now));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public DailyCheckIn markAwarded(Long checkInId, Long pointTransactionId, Instant now) {
    DailyCheckIn record = repository.findById(checkInId).orElseThrow(() ->
        new IncentiveBusinessException(
            "CHECK_IN_NOT_FOUND", "签到记录不存在", HttpStatus.NOT_FOUND));
    record.markAwarded(pointTransactionId, now);
    return record;
  }
}
