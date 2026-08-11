package com.incentive.award.repository;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardRepository extends JpaRepository<Award, Long> {
  Optional<Award> findByIdAndStatusNot(Long id, AwardStatus status);
  List<Award> findByStatusNotOrderByIdAsc(AwardStatus status);
  boolean existsByCode(String code);
}
