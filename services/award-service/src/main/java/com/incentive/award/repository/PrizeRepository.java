package com.incentive.award.repository;

import com.incentive.award.domain.Prize;
import com.incentive.award.domain.PrizeStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeRepository extends JpaRepository<Prize, Long> {
  boolean existsByCode(String code);
  Optional<Prize> findByIdAndDeletedAtIsNull(Long id);
  List<Prize> findByDeletedAtIsNullOrderByCreatedAtDesc();
  List<Prize> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(PrizeStatus status);
}
