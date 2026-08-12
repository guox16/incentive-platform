package com.incentive.award.repository;

import com.incentive.award.domain.PrizeInventoryLedger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeInventoryLedgerRepository extends JpaRepository<PrizeInventoryLedger, Long> {
  Optional<PrizeInventoryLedger> findByBusinessNo(String businessNo);
  List<PrizeInventoryLedger> findByPrizeIdOrderByCreatedAtDesc(Long prizeId);
}
