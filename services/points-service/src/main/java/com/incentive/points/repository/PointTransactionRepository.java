package com.incentive.points.repository;

import com.incentive.points.domain.PointTransaction;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, String> {
  Optional<PointTransaction> findByBusinessId(String businessId);
  Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
