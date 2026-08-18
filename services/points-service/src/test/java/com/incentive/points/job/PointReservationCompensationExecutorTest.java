package com.incentive.points.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incentive.points.domain.PointAccount;
import com.incentive.points.domain.PointReservation;
import com.incentive.points.domain.PointReservationStatus;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointReservationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(PointReservationCompensationExecutor.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PointReservationCompensationExecutorTest {
  private static final Long USER_ID = 1L;
  private static final Long BUSINESS_ID = 9001L;

  @Autowired private PointReservationCompensationExecutor executor;
  @Autowired private PointReservationRepository reservationRepository;
  @Autowired private PointAccountRepository accountRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    reservationRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void expiresAndRefundsOnlyOnce() {
    saveExpiredReservation(true);

    assertThat(executor.expireAndRefund(BUSINESS_ID)).isTrue();
    assertThat(executor.expireAndRefund(BUSINESS_ID)).isFalse();

    PointReservation reservation = reservationRepository.findByBusinessId(BUSINESS_ID).orElseThrow();
    assertThat(reservation.getStatus()).isEqualTo(PointReservationStatus.EXPIRED);
    assertThat(reservation.getExpiredAt()).isNotNull();
    assertThat(accountRepository.findById(USER_ID).orElseThrow().getBalance()).isEqualTo(100);
  }

  @Test
  void rollsBackExpiredStateWhenRefundFails() {
    saveExpiredReservation(false);

    assertThatThrownBy(() -> executor.expireAndRefund(BUSINESS_ID))
        .hasMessage("过期预占积分退回失败");

    assertThat(reservationRepository.findByBusinessId(BUSINESS_ID).orElseThrow().getStatus())
        .isEqualTo(PointReservationStatus.RESERVED);
  }

  private void saveExpiredReservation(boolean withAccount) {
    Instant now = Instant.now();
    if (withAccount) {
      PointAccount account = new PointAccount(USER_ID);
      account.credit(60);
      accountRepository.saveAndFlush(account);
    }
    reservationRepository.saveAndFlush(new PointReservation(
        BUSINESS_ID, USER_ID, 40, "LOTTERY", null,
        100, 60, now.plusSeconds(60), now));
    jdbcTemplate.update(
        "update point_reservations set expires_at = ? where business_id = ?",
        Timestamp.from(now.minusSeconds(1)), BUSINESS_ID);
  }
}
