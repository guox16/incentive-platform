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

public interface PointAccountRepository extends JpaRepository<PointAccount, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select account from PointAccount account where account.userId = :userId")
  Optional<PointAccount> findByUserIdForUpdate(@Param("userId") String userId);

  /** MySQL 幂等插入使并发首次入账只创建一个账户。 */
  @Modifying
  @Query(value = "insert ignore into point_accounts (user_id, balance, version, created_at, updated_at) "
      + "values (:userId, 0, 0, :now, :now)", nativeQuery = true)
  int createIfAbsent(@Param("userId") String userId, @Param("now") Instant now);
}
