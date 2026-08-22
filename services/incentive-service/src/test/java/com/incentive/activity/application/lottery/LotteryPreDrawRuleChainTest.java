package com.incentive.activity.application.lottery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.support.IncentiveBusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class LotteryPreDrawRuleChainTest {
  private LotteryPreDrawRuleChain chain;

  @BeforeEach
  void setUp() {
    chain = new LotteryPreDrawRuleChain(List.of(
        new UserListPreDrawRule(),
        new PrizeUnlockPreDrawRule(),
        new PointsWeightPreDrawRule()));
  }

  @Test
  void usesBasePoolWhenActivityHasNoConfiguredRules() {
    LotteryPrize first = prize(11L, 101L, PrizeType.VIRTUAL, 10, 1);
    LotteryPrize second = prize(12L, 102L, PrizeType.NONE, 20, 2);

    var result = chain.resolve(List.of(), context(7L, 10, 1), List.of(first, second));

    assertThat(result.designated()).isFalse();
    assertThat(result.pool()).extracting(entry -> entry.prize().getId())
        .containsExactly(11L, 12L);
    assertThat(result.pool()).extracting("weight").containsExactly(10L, 20L);
  }

  @Test
  void listRuleDesignatesMaximumWeightPrizeAndStopsChain() {
    LotteryPrize maximum = prize(11L, 101L, PrizeType.VIRTUAL, 50, 1);
    LotteryPrize other = prize(12L, 102L, PrizeType.NONE, 10, 2);
    List<LotteryPreDrawRuleDefinition> rules = List.of(
        userListRule(10, 7L),
        unlockRule(20, Map.of(101L, 99L)));

    var result = chain.resolve(rules, context(7L, 10, 1), List.of(maximum, other));

    assertThat(result.designatedPrize()).isSameAs(maximum);
    assertThat(result.designatedBy()).isEqualTo("USER_LIST");
  }

  @Test
  void unlocksByCurrentDrawNumberThenAdjustsWeightsForPointsTier() {
    List<LotteryPreDrawRuleDefinition> rules = List.of(
        unlockRule(10, Map.of(101L, 3L, 102L, 4L)),
        pointsRule(20, 10, Map.of(101L, new BigDecimal("2.5"))));
    LotteryPrize unlocked = prize(11L, 101L, PrizeType.VIRTUAL, 10, 1);
    LotteryPrize locked = prize(12L, 102L, PrizeType.VIRTUAL, 30, 2);

    var result = chain.resolve(rules, context(7L, 10, 3), List.of(unlocked, locked));

    assertThat(result.pool()).hasSize(1);
    assertThat(result.pool().getFirst().prize()).isSameAs(unlocked);
    assertThat(result.pool().getFirst().weight()).isEqualTo(25L);
  }

  @Test
  void designatesConfiguredLuckyPrizeWhenRulesRemoveEveryCandidate() {
    List<LotteryPreDrawRuleDefinition> rules = List.of(
        unlockRule(10, Map.of(101L, 2L, 999L, 2L)),
        new LotteryPreDrawRuleDefinition("LUCKY_FALLBACK", Integer.MAX_VALUE, true,
            new LotteryPreDrawRuleDefinition.LuckyFallbackParameters(999L)));
    LotteryPrize locked = prize(11L, 101L, PrizeType.VIRTUAL, 10, 1);
    LotteryPrize lucky = prize(12L, 999L, PrizeType.NONE, 1, 2);

    var result = chain.resolve(rules, context(7L, 10, 1), List.of(locked, lucky));

    assertThat(result.designatedPrize()).isSameAs(lucky);
    assertThat(result.designatedBy()).isEqualTo("LUCKY_FALLBACK");
  }

  @Test
  void rejectsUnknownConfiguredRule() {
    var definition = new LotteryPreDrawRuleDefinition(
        "UNKNOWN", 10, true, new LotteryPreDrawRuleDefinition.UserListParameters(Set.of(7L)));

    assertThatThrownBy(() -> chain.validateConfiguration(List.of(definition), null))
        .isInstanceOfSatisfying(IncentiveBusinessException.class,
            error -> assertThat(error.getCode()).isEqualTo("LOTTERY_PRE_RULE_INVALID"));
  }

  private LotteryPreDrawRuleDefinition userListRule(int order, Long... userIds) {
    return new LotteryPreDrawRuleDefinition("USER_LIST", order, true,
        new LotteryPreDrawRuleDefinition.UserListParameters(Set.of(userIds)));
  }

  private LotteryPreDrawRuleDefinition unlockRule(int order, Map<Long, Long> counts) {
    return new LotteryPreDrawRuleDefinition("PRIZE_UNLOCK", order, true,
        new LotteryPreDrawRuleDefinition.PrizeUnlockParameters(counts));
  }

  private LotteryPreDrawRuleDefinition pointsRule(
      int order, long minimumPoints, Map<Long, BigDecimal> multipliers) {
    return new LotteryPreDrawRuleDefinition("POINTS_WEIGHT", order, true,
        new LotteryPreDrawRuleDefinition.PointsWeightParameters(
            List.of(new LotteryPreDrawRuleDefinition.PointsTier(
                minimumPoints, multipliers))));
  }

  private LotteryPreDrawContext context(Long userId, long pointsCost, long drawNumber) {
    return new LotteryPreDrawContext(1L, userId, pointsCost, drawNumber);
  }

  private LotteryPrize prize(
      Long id, Long prizeId, PrizeType type, long weight, int displayOrder) {
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", id);
    ReflectionTestUtils.setField(prize, "activityId", 1L);
    ReflectionTestUtils.setField(prize, "ruleId", 2L);
    ReflectionTestUtils.setField(prize, "prizeId", prizeId);
    ReflectionTestUtils.setField(prize, "prizeName", "奖品" + prizeId);
    ReflectionTestUtils.setField(prize, "prizeType", type);
    ReflectionTestUtils.setField(prize, "weight", weight);
    ReflectionTestUtils.setField(prize, "displayOrder", displayOrder);
    return prize;
  }
}
