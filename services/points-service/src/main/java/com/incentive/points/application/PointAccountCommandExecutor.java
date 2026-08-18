package com.incentive.points.application;

import com.incentive.points.domain.InsufficientPointsException;
import com.incentive.points.domain.PointAccount;
import com.incentive.points.domain.PointTransaction;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 在独立本地事务中执行余额原子更新与流水写入。 */
@Component
class PointAccountCommandExecutor {
  private final PointAccountRepository accountRepository;
  private final PointTransactionRepository transactionRepository;

  PointAccountCommandExecutor(PointAccountRepository accountRepository,
      PointTransactionRepository transactionRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  PointTransaction credit(NormalizedPointCommand command) {
    Instant now = Instant.now();
    long maxBalanceBefore = Long.MAX_VALUE - command.amount();
    int updated = accountRepository.creditAtomically(
        command.userId(), command.amount(), maxBalanceBefore, now);
    if (updated == 0 && !accountRepository.existsById(command.userId())) {
      accountRepository.createIfAbsent(command.userId(), now);
      updated = accountRepository.creditAtomically(
          command.userId(), command.amount(), maxBalanceBefore, now);
    }
    if (updated == 0) {
      throw new PointBusinessException(
          "POINTS_BALANCE_OVERFLOW", "积分余额超过允许范围", HttpStatus.CONFLICT);
    }

    PointAccount account = findAccount(command.userId());
    long after = account.getBalance();
    return saveTransaction(command, after - command.amount(), after);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  PointTransaction debit(NormalizedPointCommand command) {
    Instant now = Instant.now();
    if (accountRepository.debitAtomically(command.userId(), command.amount(), now) == 0) {
      throw new InsufficientPointsException();
    }

    PointAccount account = findAccount(command.userId());
    long after = account.getBalance();
    return saveTransaction(command, Math.addExact(after, command.amount()), after);
  }

  private PointAccount findAccount(Long userId) {
    return accountRepository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("原子更新后的积分账户不存在"));
  }

  private PointTransaction saveTransaction(
      NormalizedPointCommand command, long before, long after) {
    PointTransaction transaction = new PointTransaction(
        command.businessId(), command.userId(), command.type(), command.amount(), before, after,
        command.source(), command.remark());
    try {
      return transactionRepository.saveAndFlush(transaction);
    } catch (DataIntegrityViolationException ex) {
      // 让事务代理先回滚本次余额更新，再由外层查询已经提交的同业务号流水。
      throw new PointCommandRaceException(ex);
    }
  }
}
