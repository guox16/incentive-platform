package com.incentive.points.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incentive.points.domain.PointReservation;
import com.incentive.points.domain.PointReservationStatus;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PointReservationRepositoryTest {
  private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

  @Autowired private PointReservationRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

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

  @Test
  void changesStateOnlyOnceWithConditionalUpdates() {
    repository.saveAndFlush(reservation(1001L, 1L));

    assertThat(repository.confirmAtomically(1001L, NOW.plusSeconds(10))).isEqualTo(1);
    assertThat(repository.confirmAtomically(1001L, NOW.plusSeconds(20))).isZero();
    assertThat(repository.cancelAtomically(1001L, NOW.plusSeconds(20))).isZero();
  }

  @Test
  void refusesToConfirmExpiredReservation() {
    repository.saveAndFlush(reservation(1001L, 1L));

    assertThat(repository.confirmAtomically(1001L, NOW.plusSeconds(60))).isZero();
  }

  @Test
  void partitionsExpiredReservationsAcrossSchedulerShards() {
    repository.save(reservation(1001L, 1L));
    repository.save(reservation(1002L, 2L));
    repository.save(reservation(1003L, 3L));
    repository.flush();
    jdbcTemplate.update(
        "update point_reservations set expires_at = ?",
        Timestamp.from(Instant.now().minusSeconds(1)));

    var shardZero = repository.findExpiredBusinessIdsForShard(0, 2, PageRequest.of(0, 10));
    var shardOne = repository.findExpiredBusinessIdsForShard(1, 2, PageRequest.of(0, 10));
    var all = new HashSet<>(shardZero);
    all.addAll(shardOne);

    assertThat(shardZero).doesNotContainAnyElementsOf(shardOne);
    assertThat(all).containsExactlyInAnyOrder(1001L, 1002L, 1003L);
  }

  private PointReservation reservation(Long businessId, Long userId) {
    return reservation(businessId, userId, NOW.plusSeconds(60));
  }

  private PointReservation reservation(Long businessId, Long userId, Instant expiresAt) {
    return new PointReservation(
        businessId, userId, 10, "LOTTERY", null, 100, 90, expiresAt, NOW);
  }
}
