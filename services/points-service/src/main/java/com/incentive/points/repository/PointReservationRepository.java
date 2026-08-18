package com.incentive.points.repository;

import com.incentive.points.domain.PointReservation;
import com.incentive.points.domain.PointReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointReservationRepository extends JpaRepository<PointReservation, Long> {
  /** 根据跨服务业务号查询预占记录。 */
  Optional<PointReservation> findByBusinessId(Long businessId);

  /** 查询一批等待恢复处理的过期预占记录。 */
  List<PointReservation> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
      PointReservationStatus status, Instant expiresAt);
}
