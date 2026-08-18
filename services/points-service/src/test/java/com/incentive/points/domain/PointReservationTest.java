package com.incentive.points.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PointReservationTest {
  private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

  @Test
  void confirmsReservationIdempotently() {
    PointReservation reservation = reservation();

    reservation.confirm(88L, NOW.plusSeconds(10));
    reservation.confirm(88L, NOW.plusSeconds(20));

    assertThat(reservation.getStatus()).isEqualTo(PointReservationStatus.CONFIRMED);
    assertThat(reservation.getConfirmedTransactionId()).isEqualTo(88L);
    assertThat(reservation.getConfirmedAt()).isEqualTo(NOW.plusSeconds(10));
  }

  @Test
  void cancelledReservationCannotBeConfirmed() {
    PointReservation reservation = reservation();
    reservation.cancel(NOW.plusSeconds(10));

    assertThatThrownBy(() -> reservation.confirm(88L, NOW.plusSeconds(20)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void onlyExpiresReservationAfterDeadline() {
    PointReservation reservation = reservation();

    assertThatThrownBy(() -> reservation.expire(NOW.plusSeconds(59)))
        .isInstanceOf(IllegalStateException.class);

    reservation.expire(NOW.plusSeconds(60));
    assertThat(reservation.getStatus()).isEqualTo(PointReservationStatus.EXPIRED);
    assertThat(reservation.getExpiredAt()).isEqualTo(NOW.plusSeconds(60));
    assertThat(reservation.getCancelledAt()).isNull();
  }

  private PointReservation reservation() {
    return new PointReservation(1001L, 1L, 10, "LOTTERY", "抽奖积分预占",
        NOW.plusSeconds(60), NOW);
  }
}
