package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryPrize;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotteryPrizeRepository extends JpaRepository<LotteryPrize, Long> {
  List<LotteryPrize> findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(Long activityId, Long ruleId);
  Optional<LotteryPrize> findByIdAndActivityIdAndRuleId(Long id, Long activityId, Long ruleId);

  @Query("""
      select distinct prize.prizeId from LotteryPrize prize
      where prize.activityId <> :activityId
        and prize.ruleId = (
          select rule.id from ParticipationRule rule
          where rule.activityId = prize.activityId
            and rule.ruleVersion = (
              select max(latest.ruleVersion) from ParticipationRule latest
              where latest.activityId = prize.activityId
            )
        )
      """)
  Set<Long> findPrizeIdsAssignedToOtherLatestPools(@Param("activityId") Long activityId);
}
