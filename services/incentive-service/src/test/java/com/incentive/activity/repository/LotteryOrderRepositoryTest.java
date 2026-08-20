package com.incentive.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryOrder;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class LotteryOrderRepositoryTest {
  private static final Instant NOW = Instant.parse("2026-08-20T00:30:00Z");
  @Autowired private LotteryOrderRepository repository;

  @Test
  void findsDueRetryAndStaleIntermediateOrder() {
    LotteryOrder due = order(7001L, "request-1", 9001L);
    due.scheduleRetry("POINTS_SERVICE_UNAVAILABLE", NOW.plusSeconds(5), NOW);
    LotteryOrder stale = order(7002L, "request-2", 9002L);
    repository.saveAndFlush(due);
    repository.saveAndFlush(stale);

    var ids = repository.findReconciliationOrderIds(
        NOW.plusSeconds(40), NOW.plusSeconds(10), 0, 1, PageRequest.of(0, 10));

    assertThat(ids).containsExactlyInAnyOrder(7001L, 7002L);
  }

  private LotteryOrder order(Long id, String requestId, Long pointsBusinessId) {
    IncentiveActivity activity = BeanUtils.instantiateClass(IncentiveActivity.class);
    ReflectionTestUtils.setField(activity, "id", 1L);
    ReflectionTestUtils.setField(activity, "code", "SUMMER_LOTTERY");
    ReflectionTestUtils.setField(activity, "type", ActivityType.LOTTERY);
    ReflectionTestUtils.setField(activity, "status", ActivityStatus.ACTIVE);
    ParticipationRule rule = BeanUtils.instantiateClass(ParticipationRule.class);
    ReflectionTestUtils.setField(rule, "id", 2L);
    ReflectionTestUtils.setField(rule, "ruleVersion", 1);
    ReflectionTestUtils.setField(rule, "pointsCost", 10L);
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", 31L);
    ReflectionTestUtils.setField(prize, "prizeId", 131L);
    ReflectionTestUtils.setField(prize, "prizeName", "优惠券");
    ReflectionTestUtils.setField(prize, "prizeType", PrizeType.VIRTUAL);
    return new LotteryOrder(id, requestId, 7L, activity, rule, prize,
        pointsBusinessId, "{\"passed\":true}", NOW);
  }
}
