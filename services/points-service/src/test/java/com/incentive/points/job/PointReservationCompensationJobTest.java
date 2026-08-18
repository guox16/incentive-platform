package com.incentive.points.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.points.repository.PointReservationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PointReservationCompensationJobTest {
  private final PointReservationRepository repository = mock(PointReservationRepository.class);
  private final PointReservationCompensationExecutor executor =
      mock(PointReservationCompensationExecutor.class);

  @Test
  void processesOnlyCurrentShardUpToRunLimit() {
    PointReservationCompensationJob job =
        new PointReservationCompensationJob(repository, executor, 2, 3);
    when(repository.findExpiredBusinessIdsForShard(eq(1), eq(3), any(Pageable.class)))
        .thenReturn(List.of(11L, 14L), List.of(17L));
    when(executor.expireAndRefund(11L)).thenReturn(true);
    when(executor.expireAndRefund(14L)).thenReturn(false);
    when(executor.expireAndRefund(17L)).thenReturn(true);

    var result = job.executeShard(1, 3);

    assertThat(result.scanned()).isEqualTo(3);
    assertThat(result.refunded()).isEqualTo(2);
    assertThat(result.skipped()).isEqualTo(1);
    assertThat(result.failed()).isZero();
    verify(executor).expireAndRefund(17L);
  }

  @Test
  void recordsSingleItemFailureWithoutStoppingRemainingItems() {
    PointReservationCompensationJob job =
        new PointReservationCompensationJob(repository, executor, 10, 10);
    when(repository.findExpiredBusinessIdsForShard(eq(0), eq(1), any(Pageable.class)))
        .thenReturn(List.of(21L, 22L));
    when(executor.expireAndRefund(21L)).thenThrow(new IllegalStateException("database error"));
    when(executor.expireAndRefund(22L)).thenReturn(true);

    var result = job.executeShard(0, 1);

    assertThat(result.failed()).isEqualTo(1);
    assertThat(result.refunded()).isEqualTo(1);
  }
}
