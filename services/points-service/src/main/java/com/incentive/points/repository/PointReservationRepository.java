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
import org.springframework.data.domain.Pageable;

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

  /** 查询当前 XXL-JOB 分片内一批已经到期的待确认预占。 */
  @Query(value = "select business_id from point_reservations "
      + "where status = 'RESERVED' and expires_at <= current_timestamp(3) "
      + "and mod(id, :shardTotal) = :shardIndex order by expires_at, id", nativeQuery = true)
  List<Long> findExpiredBusinessIdsForShard(
      @Param("shardIndex") int shardIndex,
      @Param("shardTotal") int shardTotal,
      Pageable pageable);

  /** 通过数据库时间和状态条件原子抢占一条过期补偿任务。 */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update point_reservations "
      + "set status = 'EXPIRED', expired_at = current_timestamp(3), "
      + "updated_at = current_timestamp(3), version = version + 1 "
      + "where business_id = :businessId and status = 'RESERVED' "
      + "and expires_at <= current_timestamp(3)", nativeQuery = true)
  int expireAtomically(@Param("businessId") Long businessId);

  /** 查询一批等待恢复处理的过期预占记录。 */
  List<PointReservation> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
      PointReservationStatus status, Instant expiresAt);
}
