package com.incentive.award.repository;

import com.incentive.award.domain.AwardIssuance;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AwardIssuanceRepository extends JpaRepository<AwardIssuance, Long> {
  Optional<AwardIssuance> findByCommandKey(String commandKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select issuance from AwardIssuance issuance where issuance.id = :id")
  Optional<AwardIssuance> findByIdForUpdate(@Param("id") Long id);
}
