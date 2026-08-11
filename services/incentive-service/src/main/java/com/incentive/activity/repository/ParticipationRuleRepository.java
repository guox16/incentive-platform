package com.incentive.activity.repository;

import com.incentive.activity.domain.ParticipationRule;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRuleRepository extends JpaRepository<ParticipationRule, Long> {
  Optional<ParticipationRule>
      findFirstByActivityIdAndStatusAndEffectiveFromLessThanEqualOrderByRuleVersionDesc(
          Long activityId, ParticipationRule.Status status, Instant now);
}
