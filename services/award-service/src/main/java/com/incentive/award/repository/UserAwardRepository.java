package com.incentive.award.repository;

import com.incentive.award.domain.UserAward;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAwardRepository extends JpaRepository<UserAward, Long> {
  Optional<UserAward> findByIssuanceId(Long issuanceId);

  Page<UserAward> findByUserIdOrderByObtainedAtDescIdDesc(Long userId, Pageable page);
}
