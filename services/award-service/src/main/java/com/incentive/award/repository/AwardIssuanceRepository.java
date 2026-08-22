package com.incentive.award.repository;

import com.incentive.award.domain.AwardIssuance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardIssuanceRepository extends JpaRepository<AwardIssuance, Long> {
  Optional<AwardIssuance> findByCommandKey(String commandKey);
}
