package com.incentive.points.repository;

import com.incentive.points.domain.PointAccount;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {
  /** 使用悲观锁查询用户积分账户。 */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select account from PointAccount account where account.userId = :userId")
  Optional<PointAccount> findByUserIdForUpdate(@Param("userId") Long userId);

  /** MySQL 幂等插入使并发首次入账只创建一个账户。 */
  /** 幂等地创建积分账户，已存在时不做变更。 */
  @Modifying
  @Query(value = "insert ignore into point_accounts (user_id, balance, version, created_at, updated_at) "
      + "values (:userId, 0, 0, :now, :now)", nativeQuery = true)
  int createIfAbsent(@Param("userId") Long userId, @Param("now") Instant now);
}
