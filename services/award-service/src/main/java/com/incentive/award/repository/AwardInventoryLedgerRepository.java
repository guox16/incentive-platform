package com.incentive.award.repository;

import com.incentive.award.domain.AwardInventoryLedger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AwardInventoryLedgerRepository
    extends JpaRepository<AwardInventoryLedger, Long> {
  Optional<AwardInventoryLedger> findByBusinessNo(String businessNo);
  List<AwardInventoryLedger> findByAwardIdOrderByCreatedAtDesc(Long awardId);
}
