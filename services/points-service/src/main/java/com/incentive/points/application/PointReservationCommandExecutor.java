package com.incentive.points.application;

import com.incentive.points.domain.InsufficientPointsException;
import com.incentive.points.domain.PointAccount;
import com.incentive.points.domain.PointReservation;
import com.incentive.points.domain.PointTransaction;
import com.incentive.points.domain.PointTransactionType;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointReservationRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 在独立本地事务中执行预占余额变更和状态条件更新。 */
@Component
class PointReservationCommandExecutor {
  private final PointAccountRepository accountRepository;
  private final PointReservationRepository reservationRepository;
  private final PointTransactionRepository transactionRepository;

  PointReservationCommandExecutor(PointAccountRepository accountRepository,
      PointReservationRepository reservationRepository,
      PointTransactionRepository transactionRepository) {
    this.accountRepository = accountRepository;
    this.reservationRepository = reservationRepository;
    this.transactionRepository = transactionRepository;
  }

  /** 原子扣减可用余额并创建待确认预占。 */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  PointReservation reserve(NormalizedPointReservationCommand command) {
    Instant now = now();
    if (!command.expiresAt().isAfter(now)) {
      throw new PointBusinessException(
          "POINT_RESERVATION_EXPIRES_AT_INVALID", "预占过期时间必须晚于当前时间", HttpStatus.BAD_REQUEST);
    }
    if (transactionRepository.findByBusinessId(command.businessId()).isPresent()) {
      throw idempotencyKeyReused();
    }
    if (accountRepository.debitAtomically(command.userId(), command.amount(), now) == 0) {
      throw new InsufficientPointsException();
    }

    PointAccount account = findAccount(command.userId());
    long after = account.getBalance();
    PointReservation reservation = new PointReservation(
        command.businessId(), command.userId(), command.amount(), command.source(), command.remark(),
        Math.addExact(after, command.amount()), after, command.expiresAt(), now);
    try {
      return reservationRepository.saveAndFlush(reservation);
    } catch (DataIntegrityViolationException ex) {
      // 唯一键竞争时回滚本次余额扣减，外层再读取胜出的预占记录。
      throw new PointCommandRaceException(ex);
    }
  }

  /** 通过状态条件更新确认预占，并生成正式扣减流水。 */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  PointReservation confirm(Long businessId) {
    PointReservation reservation = requireReservation(businessId);
    Instant now = now();
    if (reservationRepository.confirmAtomically(businessId, now) == 0) {
      throw new PointCommandRaceException();
    }

    PointTransaction transaction = new PointTransaction(
        reservation.getBusinessId(), reservation.getUserId(), PointTransactionType.DEBIT,
        reservation.getAmount(), reservation.getBalanceBefore(), reservation.getBalanceAfter(),
        reservation.getSource(), reservation.getRemark());
    try {
      transaction = transactionRepository.saveAndFlush(transaction);
    } catch (DataIntegrityViolationException ex) {
      throw new PointCommandRaceException(ex);
    }
    if (reservationRepository.attachConfirmedTransaction(businessId, transaction.getId()) != 1) {
      throw new IllegalStateException("确认积分预占后无法关联扣减流水");
    }
    return requireReservation(businessId);
  }

  /** 通过状态条件更新取消预占，并在同一事务退回积分。 */
  @Transactional(isolation = Isolation.READ_COMMITTED)
  PointReservation cancel(Long businessId) {
    requireReservation(businessId);
    Instant now = now();
    if (reservationRepository.cancelAtomically(businessId, now) == 0) {
      throw new PointCommandRaceException();
    }

    PointReservation cancelled = requireReservation(businessId);
    long maxBalanceBefore = Long.MAX_VALUE - cancelled.getAmount();
    if (accountRepository.creditAtomically(
        cancelled.getUserId(), cancelled.getAmount(), maxBalanceBefore, now) != 1) {
      throw new PointBusinessException(
          "POINTS_BALANCE_OVERFLOW", "取消预占后积分余额超过允许范围", HttpStatus.CONFLICT);
    }
    return requireReservation(businessId);
  }

  private PointReservation requireReservation(Long businessId) {
    return reservationRepository.findByBusinessId(businessId)
        .orElseThrow(() -> new PointBusinessException(
            "POINT_RESERVATION_NOT_FOUND", "积分预占不存在", HttpStatus.NOT_FOUND));
  }

  private PointAccount findAccount(Long userId) {
    return accountRepository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("预占扣减后的积分账户不存在"));
  }

  private PointBusinessException idempotencyKeyReused() {
    return new PointBusinessException(
        "IDEMPOTENCY_KEY_REUSED", "业务号已被其他积分命令使用", HttpStatus.CONFLICT);
  }

  private Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS);
  }
}
