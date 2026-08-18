package com.incentive.points.job;

import com.incentive.points.domain.PointReservation;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointReservationRepository;
import com.incentive.points.support.PointBusinessException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 在独立本地事务中抢占并补偿单条过期积分预占。 */
@Component
public class PointReservationCompensationExecutor {
  private final PointReservationRepository reservationRepository;
  private final PointAccountRepository accountRepository;

  public PointReservationCompensationExecutor(PointReservationRepository reservationRepository,
      PointAccountRepository accountRepository) {
    this.reservationRepository = reservationRepository;
    this.accountRepository = accountRepository;
  }

  /** 只有成功将 RESERVED 改为 EXPIRED 的实例才能退回积分。 */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public boolean expireAndRefund(Long businessId) {
    if (reservationRepository.expireAtomically(businessId) == 0) return false;

    PointReservation reservation = reservationRepository.findByBusinessId(businessId)
        .orElseThrow(() -> new IllegalStateException("已抢占的积分预占不存在"));
    long maxBalanceBefore = Long.MAX_VALUE - reservation.getAmount();
    if (accountRepository.creditAtomically(
        reservation.getUserId(), reservation.getAmount(), maxBalanceBefore, Instant.now()) != 1) {
      throw new PointBusinessException(
          "POINTS_COMPENSATION_FAILED", "过期预占积分退回失败", HttpStatus.CONFLICT);
    }
    return true;
  }
}
