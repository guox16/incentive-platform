package com.incentive.award.repository;

import com.incentive.award.domain.ShopRedemption;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRedemptionRepository extends JpaRepository<ShopRedemption, Long> {
  Optional<ShopRedemption> findByUserIdAndRequestId(Long userId, String requestId);

  List<ShopRedemption> findByUserIdOrderByIdDesc(Long userId, Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select redemption from ShopRedemption redemption where redemption.id = :id")
  Optional<ShopRedemption> findForUpdate(@Param("id") Long id);

  @Query("""
      select redemption.id from ShopRedemption redemption
      where redemption.status in :statuses and redemption.nextAttemptAt <= :now
      order by redemption.nextAttemptAt, redemption.id
      """)
  List<Long> findDueIds(@Param("statuses") Collection<ShopRedemption.Status> statuses,
      @Param("now") Instant now, Pageable page);
}
