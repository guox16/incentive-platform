package com.incentive.points.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.points.domain.InsufficientPointsException;
import com.incentive.points.domain.PointAccount;
import com.incentive.points.domain.PointTransaction;
import com.incentive.points.domain.PointTransactionType;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PointAccountCommandExecutorTest {
  @Mock private PointAccountRepository accountRepository;
  @Mock private PointTransactionRepository transactionRepository;
  private PointAccountCommandExecutor executor;

  @BeforeEach
  void setUp() {
    executor = new PointAccountCommandExecutor(accountRepository, transactionRepository);
  }

  @Test
  void creditsExistingAccountWithAtomicUpdate() {
    NormalizedPointCommand command = command(PointTransactionType.CREDIT, 40);
    PointAccount account = accountWithBalance(100);
    when(accountRepository.creditAtomically(
        eq(1L), eq(40L), eq(Long.MAX_VALUE - 40), any(Instant.class))).thenReturn(1);
    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(transactionRepository.saveAndFlush(any(PointTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PointTransaction result = executor.credit(command);

    assertThat(result.getBalanceBefore()).isEqualTo(60);
    assertThat(result.getBalanceAfter()).isEqualTo(100);
  }

  @Test
  void createsMissingAccountThenRetriesAtomicCredit() {
    NormalizedPointCommand command = command(PointTransactionType.CREDIT, 40);
    PointAccount account = accountWithBalance(40);
    when(accountRepository.creditAtomically(
        eq(1L), eq(40L), eq(Long.MAX_VALUE - 40), any(Instant.class)))
        .thenReturn(0, 1);
    when(accountRepository.existsById(1L)).thenReturn(false);
    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(transactionRepository.saveAndFlush(any(PointTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PointTransaction result = executor.credit(command);

    assertThat(result.getBalanceBefore()).isZero();
    assertThat(result.getBalanceAfter()).isEqualTo(40);
    verify(accountRepository).createIfAbsent(eq(1L), any(Instant.class));
  }

  @Test
  void rejectsCreditOverflow() {
    NormalizedPointCommand command = command(PointTransactionType.CREDIT, 1);
    when(accountRepository.creditAtomically(
        eq(1L), eq(1L), eq(Long.MAX_VALUE - 1), any(Instant.class))).thenReturn(0);
    when(accountRepository.existsById(1L)).thenReturn(true);

    assertThatThrownBy(() -> executor.credit(command))
        .isInstanceOf(PointBusinessException.class)
        .extracting("code").isEqualTo("POINTS_BALANCE_OVERFLOW");
  }

  @Test
  void debitsAccountWithAtomicConditionalUpdate() {
    NormalizedPointCommand command = command(PointTransactionType.DEBIT, 40);
    PointAccount account = accountWithBalance(60);
    when(accountRepository.debitAtomically(eq(1L), eq(40L), any(Instant.class))).thenReturn(1);
    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(transactionRepository.saveAndFlush(any(PointTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PointTransaction result = executor.debit(command);

    assertThat(result.getBalanceBefore()).isEqualTo(100);
    assertThat(result.getBalanceAfter()).isEqualTo(60);
  }

  @Test
  void rejectsDebitWhenConditionalUpdateDoesNotMatch() {
    NormalizedPointCommand command = command(PointTransactionType.DEBIT, 40);
    when(accountRepository.debitAtomically(eq(1L), eq(40L), any(Instant.class))).thenReturn(0);

    assertThatThrownBy(() -> executor.debit(command))
        .isInstanceOf(InsufficientPointsException.class);
  }

  @Test
  void translatesDuplicateLedgerInsertIntoRaceException() {
    NormalizedPointCommand command = command(PointTransactionType.DEBIT, 40);
    PointAccount account = accountWithBalance(60);
    when(accountRepository.debitAtomically(eq(1L), eq(40L), any(Instant.class))).thenReturn(1);
    when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
    when(transactionRepository.saveAndFlush(any(PointTransaction.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThatThrownBy(() -> executor.debit(command))
        .isInstanceOf(PointCommandRaceException.class);
  }

  private NormalizedPointCommand command(PointTransactionType type, long amount) {
    return new NormalizedPointCommand(1001L, 1L, type, amount, "TEST", null);
  }

  private PointAccount accountWithBalance(long balance) {
    PointAccount account = new PointAccount(1L);
    account.credit(balance);
    return account;
  }
}
