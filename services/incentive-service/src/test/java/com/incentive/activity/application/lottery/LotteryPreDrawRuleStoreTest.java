package com.incentive.activity.application.lottery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.activity.domain.LotteryPreDrawRuleConfig;
import com.incentive.activity.repository.LotteryPreDrawRuleConfigRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LotteryPreDrawRuleStoreTest {
  @Mock LotteryPreDrawRuleConfigRepository ruleRepository;
  LotteryPreDrawRuleStore store;

  @BeforeEach
  void setUp() {
    store = new LotteryPreDrawRuleStore(ruleRepository, new ObjectMapper());
  }

  @Test
  void loadsCompactJsonIntoTypedDefinitions() {
    when(ruleRepository.findByParticipationRuleIdOrderByExecutionOrderAscIdAsc(2L))
        .thenReturn(List.of(
            row(UserListPreDrawRule.TYPE, 10, "[7,8]"),
            row(PrizeUnlockPreDrawRule.TYPE, 20, "{\"101\":3}"),
            row(PointsWeightPreDrawRule.TYPE, 30, "{\"10\":{\"101\":1.5}}"),
            row(LotteryPreDrawRuleConfig.LUCKY_FALLBACK, Integer.MAX_VALUE, "999")));

    List<LotteryPreDrawRuleDefinition> definitions = store.load(2L);

    assertThat(((LotteryPreDrawRuleDefinition.UserListParameters)
        definitions.get(0).parameters()).userIds()).containsExactlyInAnyOrder(7L, 8L);
    assertThat(((LotteryPreDrawRuleDefinition.PrizeUnlockParameters)
        definitions.get(1).parameters()).minimumDrawCounts()).containsEntry(101L, 3L);
    assertThat(((LotteryPreDrawRuleDefinition.PointsWeightParameters)
        definitions.get(2).parameters()).tiers().getFirst().weightMultipliers())
        .containsEntry(101L, new BigDecimal("1.5"));
    assertThat(((LotteryPreDrawRuleDefinition.LuckyFallbackParameters)
        definitions.get(3).parameters()).prizeId()).isEqualTo(999L);
  }

  @Test
  void savesEveryRuleAsOneRowWithCompactJson() {
    List<LotteryPreDrawRuleDefinition> definitions = List.of(
        new LotteryPreDrawRuleDefinition(UserListPreDrawRule.TYPE, 10, true,
            new LotteryPreDrawRuleDefinition.UserListParameters(Set.of(7L, 8L))),
        new LotteryPreDrawRuleDefinition(PrizeUnlockPreDrawRule.TYPE, 20, true,
            new LotteryPreDrawRuleDefinition.PrizeUnlockParameters(Map.of(101L, 3L))),
        new LotteryPreDrawRuleDefinition(PointsWeightPreDrawRule.TYPE, 30, true,
            new LotteryPreDrawRuleDefinition.PointsWeightParameters(List.of(
                new LotteryPreDrawRuleDefinition.PointsTier(
                    10, Map.of(101L, new BigDecimal("1.5")))))),
        new LotteryPreDrawRuleDefinition(LotteryPreDrawRuleConfig.LUCKY_FALLBACK,
            Integer.MAX_VALUE, true,
            new LotteryPreDrawRuleDefinition.LuckyFallbackParameters(999L)));

    store.save(1L, 2L, definitions);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<LotteryPreDrawRuleConfig>> captor =
        ArgumentCaptor.forClass(Iterable.class);
    verify(ruleRepository).saveAll(captor.capture());
    List<LotteryPreDrawRuleConfig> rows = StreamSupport
        .stream(captor.getValue().spliterator(), false).toList();
    assertThat(rows).extracting(LotteryPreDrawRuleConfig::getRuleType)
        .containsExactly("USER_LIST", "PRIZE_UNLOCK", "POINTS_WEIGHT", "LUCKY_FALLBACK");
    assertThat(rows.get(0).getRuleConfig()).contains("7", "8");
    assertThat(rows.get(1).getRuleConfig()).isEqualTo("{\"101\":3}");
    assertThat(rows.get(2).getRuleConfig()).isEqualTo("{\"10\":{\"101\":1.5}}");
    assertThat(rows.get(3).getRuleConfig()).isEqualTo("999");
  }

  private LotteryPreDrawRuleConfig row(String type, int order, String config) {
    return new LotteryPreDrawRuleConfig(1L, 2L, type, order, true, config);
  }
}
