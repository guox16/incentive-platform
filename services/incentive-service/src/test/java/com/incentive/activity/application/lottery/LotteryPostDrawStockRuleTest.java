package com.incentive.activity.application.lottery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LotteryPostDrawStockRuleTest {
  @Mock private LotteryStockReservationStore stockStore;
  private LotteryPostDrawStockRule rule;

  @BeforeEach
  void setUp() {
    rule = new LotteryPostDrawStockRule(stockStore);
  }

  @Test
  void keepsSelectedPrizeWhenStockNumberIsReserved() {
    LotteryPrize selected = prize(31L, 131L, PrizeType.VIRTUAL, 100L);
    when(stockStore.reserve(1L, 131L, 7001L, 100L)).thenReturn(17L);

    var result = rule.resolve(7001L, 1L, selected, List.of(selected), null);

    assertThat(result.prize()).isSameAs(selected);
    assertThat(result.stockNo()).isEqualTo(17L);
    assertThat(result.fallback()).isFalse();
  }

  @Test
  void fallsBackToNoneLuckyPrizeWhenSelectedStockIsEmpty() {
    LotteryPrize selected = prize(31L, 131L, PrizeType.VIRTUAL, 1L);
    LotteryPrize lucky = prize(32L, 132L, PrizeType.NONE, null);
    when(stockStore.reserve(1L, 131L, 7001L, 1L)).thenReturn(null);

    var result = rule.resolve(7001L, 1L, selected, List.of(selected, lucky), 132L);

    assertThat(result.prize()).isSameAs(lucky);
    assertThat(result.stockNo()).isNull();
    assertThat(result.fallback()).isTrue();
    verify(stockStore, never()).reserve(1L, 132L, 7001L, 0L);
  }

  @Test
  void rejectsLimitedPrizeWithoutConfiguredQuota() {
    LotteryPrize selected = prize(31L, 131L, PrizeType.VIRTUAL, null);

    assertThatThrownBy(() -> rule.resolve(7001L, 1L, selected, List.of(selected), null))
        .isInstanceOfSatisfying(IncentiveBusinessException.class,
            error -> assertThat(error.getCode()).isEqualTo("LOTTERY_POST_RULE_INVALID"));
  }

  private LotteryPrize prize(Long id, Long prizeId, PrizeType type, Long quota) {
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", id);
    ReflectionTestUtils.setField(prize, "activityId", 1L);
    ReflectionTestUtils.setField(prize, "ruleId", 2L);
    ReflectionTestUtils.setField(prize, "prizeId", prizeId);
    ReflectionTestUtils.setField(prize, "prizeName", "奖品" + prizeId);
    ReflectionTestUtils.setField(prize, "prizeType", type);
    ReflectionTestUtils.setField(prize, "campaignQuota", quota);
    return prize;
  }
}
