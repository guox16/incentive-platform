package com.incentive.points.repository;

import com.incentive.points.domain.PointTransaction;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
  /** 根据业务幂等号查询积分流水。 */
  Optional<PointTransaction> findByBusinessId(Long businessId);
  /** 按创建时间倒序分页查询用户积分流水。 */
  Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
