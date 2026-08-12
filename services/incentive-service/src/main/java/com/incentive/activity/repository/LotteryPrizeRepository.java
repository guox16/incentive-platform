package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryPrize;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryPrizeRepository extends JpaRepository<LotteryPrize, Long> {
  List<LotteryPrize> findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(Long activityId, Long ruleId);
  Optional<LotteryPrize> findByIdAndActivityIdAndRuleId(Long id, Long activityId, Long ruleId);
}
