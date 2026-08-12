package com.incentive.activity.repository;

import com.incentive.activity.domain.PendingAward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingAwardRepository extends JpaRepository<PendingAward, Long> {}
