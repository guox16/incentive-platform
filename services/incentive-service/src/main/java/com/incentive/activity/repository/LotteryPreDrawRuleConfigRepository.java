package com.incentive.activity.repository;

import com.incentive.activity.domain.LotteryPreDrawRuleConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryPreDrawRuleConfigRepository
    extends JpaRepository<LotteryPreDrawRuleConfig, Long> {
  List<LotteryPreDrawRuleConfig> findByParticipationRuleIdOrderByExecutionOrderAscIdAsc(
      Long participationRuleId);
}
