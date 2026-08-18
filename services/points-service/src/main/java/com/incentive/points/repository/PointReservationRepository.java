package com.incentive.points.repository;

import com.incentive.points.domain.PointReservation;
import com.incentive.points.domain.PointReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointReservationRepository extends JpaRepository<PointReservation, Long> {
  /** 根据跨服务业务号查询预占记录。 */
  Optional<PointReservation> findByBusinessId(Long businessId);

  /** 仅允许尚未过期的待确认预占进入已确认状态。 */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update point_reservations "
      + "set status = 'CONFIRMED', confirmed_at = :now, updated_at = :now, version = version + 1 "
      + "where business_id = :businessId and status = 'RESERVED' and expires_at > :now",
      nativeQuery = true)
  int confirmAtomically(@Param("businessId") Long businessId, @Param("now") Instant now);

  /** 在确认事务中关联刚生成的正式扣减流水。 */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update point_reservations set confirmed_transaction_id = :transactionId "
      + "where business_id = :businessId and status = 'CONFIRMED' "
      + "and confirmed_transaction_id is null", nativeQuery = true)
  int attachConfirmedTransaction(@Param("businessId") Long businessId,
      @Param("transactionId") Long transactionId);

  /** 仅允许待确认预占进入已取消状态。 */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update point_reservations "
      + "set status = 'CANCELLED', cancelled_at = :now, updated_at = :now, version = version + 1 "
      + "where business_id = :businessId and status = 'RESERVED'", nativeQuery = true)
  int cancelAtomically(@Param("businessId") Long businessId, @Param("now") Instant now);

  /** 查询一批等待恢复处理的过期预占记录。 */
  List<PointReservation> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
      PointReservationStatus status, Instant expiresAt);
}
