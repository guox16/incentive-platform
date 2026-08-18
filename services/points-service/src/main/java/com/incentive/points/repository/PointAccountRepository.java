package com.incentive.points.repository;

import com.incentive.points.domain.PointAccount;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {
  /** 幂等地创建积分账户，已存在时不做变更。 */
  @Modifying
  @Query(value = "insert ignore into point_accounts (user_id, balance, version, created_at, updated_at) "
      + "values (:userId, 0, 0, :now, :now)", nativeQuery = true)
  int createIfAbsent(@Param("userId") Long userId, @Param("now") Instant now);

  /** 使用单条条件更新原子增加余额；余额超过 Java long 范围时不更新。 */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update point_accounts "
      + "set balance = balance + :amount, version = version + 1, updated_at = :now "
      + "where user_id = :userId and balance <= :maxBalanceBefore", nativeQuery = true)
  int creditAtomically(@Param("userId") Long userId, @Param("amount") long amount,
      @Param("maxBalanceBefore") long maxBalanceBefore, @Param("now") Instant now);

  /** 使用单条条件更新原子扣减余额；余额不足或账户不存在时不更新。 */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "update point_accounts "
      + "set balance = balance - :amount, version = version + 1, updated_at = :now "
      + "where user_id = :userId and balance >= :amount", nativeQuery = true)
  int debitAtomically(@Param("userId") Long userId, @Param("amount") long amount,
      @Param("now") Instant now);
}
