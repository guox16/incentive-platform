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
import com.incentive.points.dto.PointCommandRequest;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PointAccountServiceTest {
  private static final AtomicLong BUSINESS_IDS = new AtomicLong();
  @Mock private PointAccountRepository accountRepository;
  @Mock private PointTransactionRepository transactionRepository;
  private PointAccountService service;

  @BeforeEach
  void setUp() { service = new PointAccountService(accountRepository, transactionRepository); }

  @Test
  void returnsVirtualZeroBalanceWithoutCreatingAccount() {
    Long userId = 1L;
    when(accountRepository.findById(userId)).thenReturn(Optional.empty());

    var response = service.getBalance(userId);

    assertThat(response.balance()).isZero();
    assertThat(response.accountCreated()).isFalse();
    verify(accountRepository).findById(userId);
  }

  @Test
  void firstCreditCreatesAccountAndLedger() {
    PointCommandRequest request = request(100, "award", "welcome");
    PointAccount account = new PointAccount(request.userId());
    when(transactionRepository.findByBusinessId(request.businessId())).thenReturn(Optional.empty());
    when(accountRepository.findByUserIdForUpdate(request.userId())).thenReturn(Optional.of(account));
    when(transactionRepository.saveAndFlush(any(PointTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.credit(request);

    assertThat(response.balanceBefore()).isZero();
    assertThat(response.balanceAfter()).isEqualTo(100);
    assertThat(response.source()).isEqualTo("AWARD");
    verify(accountRepository).createIfAbsent(eq(request.userId()), any(Instant.class));
  }

  @Test
  void exactDuplicateReturnsOriginalTransaction() {
    PointCommandRequest request = request(100, "award", "welcome");
    PointTransaction existing = transaction(request, PointTransactionType.CREDIT, "AWARD", "welcome");
    when(transactionRepository.findByBusinessId(request.businessId())).thenReturn(Optional.of(existing));

    var response = service.credit(request);

    assertThat(response.replayed()).isTrue();
    assertThat(response.balanceAfter()).isEqualTo(100);
  }

  @Test
  void rejectsReusedBusinessIdWithDifferentPayload() {
    PointCommandRequest request = request(100, "award", null);
    PointTransaction existing = new PointTransaction(request.businessId(), request.userId(),
        PointTransactionType.CREDIT, 20, 0, 20, "AWARD", null);
    when(transactionRepository.findByBusinessId(request.businessId())).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.credit(request)).isInstanceOf(PointBusinessException.class)
        .extracting("code").isEqualTo("IDEMPOTENCY_KEY_REUSED");
  }

  @Test
  void debitWithoutAccountIsInsufficient() {
    PointCommandRequest request = request(1, "exchange", null);
    when(transactionRepository.findByBusinessId(request.businessId())).thenReturn(Optional.empty());
    when(accountRepository.findByUserIdForUpdate(request.userId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.debit(request)).isInstanceOf(InsufficientPointsException.class);
  }

  @Test
  void debitsExistingAccountAndRecordsBalanceChange() {
    PointCommandRequest request = request(40, "exchange", "order");
    PointAccount account = new PointAccount(request.userId());
    account.credit(100);
    when(transactionRepository.findByBusinessId(request.businessId())).thenReturn(Optional.empty());
    when(accountRepository.findByUserIdForUpdate(request.userId())).thenReturn(Optional.of(account));
    when(transactionRepository.saveAndFlush(any(PointTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.debit(request);

    assertThat(response.balanceBefore()).isEqualTo(100);
    assertThat(response.balanceAfter()).isEqualTo(60);
    assertThat(response.type()).isEqualTo(PointTransactionType.DEBIT);
  }

  @Test
  void returnsPagedLedgerInRepositoryOrder() {
    Long userId = 1L;
    PointCommandRequest request = new PointCommandRequest(nextBusinessId(), userId, 10, "TASK", null);
    PointTransaction transaction = transaction(request, PointTransactionType.CREDIT, "TASK", null);
    when(transactionRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(transaction)));

    var response = service.getTransactions(userId, 0, 20);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().getFirst().businessId()).isEqualTo(request.businessId());
  }

  private PointCommandRequest request(long amount, String source, String remark) {
    return new PointCommandRequest(nextBusinessId(), 1L, amount, source, remark);
  }

  private PointTransaction transaction(PointCommandRequest request, PointTransactionType type, String source, String remark) {
    return new PointTransaction(request.businessId(), request.userId(), type,
        request.amount(), 0, request.amount(), source, remark);
  }

  private long nextBusinessId() { return BUSINESS_IDS.incrementAndGet(); }
}
