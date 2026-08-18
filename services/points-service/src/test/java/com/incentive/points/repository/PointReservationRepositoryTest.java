package com.incentive.points.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incentive.points.domain.PointReservation;
import com.incentive.points.domain.PointReservationStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PointReservationRepositoryTest {
  private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

  @Autowired private PointReservationRepository repository;

  @Test
  void enforcesUniqueBusinessId() {
    repository.saveAndFlush(reservation(1001L, 1L));

    assertThatThrownBy(() -> repository.saveAndFlush(reservation(1001L, 2L)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void findsExpiredReservedItemsInDeadlineOrder() {
    repository.save(reservation(1001L, 1L, NOW.plusSeconds(20)));
    repository.save(reservation(1002L, 2L, NOW.plusSeconds(10)));
    repository.save(reservation(1003L, 3L, NOW.plusSeconds(40)));
    repository.flush();

    var expired = repository
        .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            PointReservationStatus.RESERVED, NOW.plusSeconds(30));

    assertThat(expired).extracting(PointReservation::getBusinessId)
        .containsExactly(1002L, 1001L);
  }

  private PointReservation reservation(Long businessId, Long userId) {
    return reservation(businessId, userId, NOW.plusSeconds(60));
  }

  private PointReservation reservation(Long businessId, Long userId, Instant expiresAt) {
    return new PointReservation(businessId, userId, 10, "LOTTERY", null, expiresAt, NOW);
  }
}
