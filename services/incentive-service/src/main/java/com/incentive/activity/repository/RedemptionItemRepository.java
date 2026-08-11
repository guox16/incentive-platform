package com.incentive.activity.repository;

import com.incentive.activity.domain.RedemptionItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedemptionItemRepository extends JpaRepository<RedemptionItem, Long> {
  List<RedemptionItem> findByActivityIdAndRuleIdAndStatusOrderByDisplayOrderAscIdAsc(
      Long activityId, Long ruleId, RedemptionItem.Status status);

  Optional<RedemptionItem> findByIdAndActivityIdAndRuleIdAndStatus(
      Long id, Long activityId, Long ruleId, RedemptionItem.Status status);
}
