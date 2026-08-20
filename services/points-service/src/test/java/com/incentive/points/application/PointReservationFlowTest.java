package com.incentive.points.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incentive.points.domain.InsufficientPointsException;
import com.incentive.points.domain.PointAccount;
import com.incentive.points.domain.PointReservationStatus;
import com.incentive.points.domain.PointTransactionType;
import com.incentive.points.dto.PointReservationRequest;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointReservationRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({PointReservationCommandExecutor.class, PointReservationService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PointReservationFlowTest {
  private static final Long USER_ID = 1L;

  @Autowired private PointReservationService service;
  @Autowired private PointAccountRepository accountRepository;
  @Autowired private PointReservationRepository reservationRepository;
  @Autowired private PointTransactionRepository transactionRepository;

  @BeforeEach
  void setUp() {
    transactionRepository.deleteAll();
    reservationRepository.deleteAll();
    accountRepository.deleteAll();
    PointAccount account = new PointAccount(USER_ID);
    account.credit(100);
    accountRepository.saveAndFlush(account);
  }

  @Test
  void reservesAndConfirmsIdempotently() {
    PointReservationRequest request = request(1001L, 40);

    var reserved = service.reserve(request);
    var reserveReplay = service.reserve(request);

    assertThat(reserved.status()).isEqualTo(PointReservationStatus.RESERVED);
    assertThat(reserved.balanceBefore()).isEqualTo(100);
    assertThat(reserved.balanceAfter()).isEqualTo(60);
    assertThat(reserveReplay.replayed()).isTrue();
    assertThat(reserveReplay.expiresAt()).isEqualTo(reserved.expiresAt());
    assertThat(accountRepository.findById(USER_ID).orElseThrow().getBalance()).isEqualTo(60);

    var confirmed = service.confirm(request.businessId());
    var confirmReplay = service.confirm(request.businessId());

    assertThat(confirmed.status()).isEqualTo(PointReservationStatus.CONFIRMED);
    assertThat(confirmed.confirmedTransactionId()).isNotNull();
    assertThat(confirmReplay.replayed()).isTrue();
    assertThat(accountRepository.findById(USER_ID).orElseThrow().getBalance()).isEqualTo(60);
    var transaction = transactionRepository.findByBusinessId(request.businessId()).orElseThrow();
    assertThat(transaction.getType()).isEqualTo(PointTransactionType.DEBIT);
    assertThat(transaction.getBalanceBefore()).isEqualTo(100);
    assertThat(transaction.getBalanceAfter()).isEqualTo(60);
  }

  @Test
  void cancelsAndRestoresBalanceIdempotently() {
    PointReservationRequest request = request(1002L, 40);
    service.reserve(request);

    var cancelled = service.cancel(request.businessId());
    var replay = service.cancel(request.businessId());

    assertThat(cancelled.status()).isEqualTo(PointReservationStatus.CANCELLED);
    assertThat(replay.replayed()).isTrue();
    assertThat(accountRepository.findById(USER_ID).orElseThrow().getBalance()).isEqualTo(100);
    assertThat(transactionRepository.findByBusinessId(request.businessId())).isEmpty();
  }

  @Test
  void insufficientBalanceCreatesNoReservation() {
    PointReservationRequest request = request(1003L, 101);

    assertThatThrownBy(() -> service.reserve(request))
        .isInstanceOf(InsufficientPointsException.class);
    assertThat(reservationRepository.findByBusinessId(request.businessId())).isEmpty();
    assertThat(accountRepository.findById(USER_ID).orElseThrow().getBalance()).isEqualTo(100);
  }

  @Test
  void rejectsDifferentPayloadForExistingBusinessId() {
    PointReservationRequest request = request(1005L, 40);
    service.reserve(request);
    PointReservationRequest changed = new PointReservationRequest(
        request.businessId(), request.userId(), 41, request.source(), request.remark());

    assertThatThrownBy(() -> service.reserve(changed))
        .isInstanceOf(PointBusinessException.class)
        .extracting("code")
        .isEqualTo("IDEMPOTENCY_KEY_REUSED");
    assertThat(accountRepository.findById(USER_ID).orElseThrow().getBalance()).isEqualTo(60);
  }

  @Test
  void confirmedReservationCannotBeCancelled() {
    PointReservationRequest request = request(1004L, 40);
    service.reserve(request);
    service.confirm(request.businessId());

    assertThatThrownBy(() -> service.cancel(request.businessId()))
        .hasMessage("只有待确认的积分预占才能取消");
    assertThat(accountRepository.findById(USER_ID).orElseThrow().getBalance()).isEqualTo(60);
  }

  private PointReservationRequest request(Long businessId, long amount) {
    return new PointReservationRequest(businessId, USER_ID, amount, "lottery", "抽奖预占");
  }
}
