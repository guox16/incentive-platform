package com.incentive.award.repository;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardStatus;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardRepository extends JpaRepository<Award, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select award from Award award where award.id = :id")
  Optional<Award> findByIdWithPessimisticLock(@Param("id") Long id);

  Optional<Award> findByIdAndStatusNot(Long id, AwardStatus status);
  List<Award> findByStatusNotOrderByIdAsc(AwardStatus status);
}
