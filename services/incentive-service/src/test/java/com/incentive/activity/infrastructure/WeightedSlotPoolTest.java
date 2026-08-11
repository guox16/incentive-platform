package com.incentive.activity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.incentive.activity.domain.LotteryPrize;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class WeightedSlotPoolTest {
  @Test
  void reducesWeightsByGreatestCommonDivisor() {
    LotteryPrize first = prize(11L, 20L);
    LotteryPrize second = prize(12L, 30L);
    LotteryPrize third = prize(13L, 50L);

    List<String> slots = WeightedSlotPool.build(List.of(first, second, third));

    assertThat(slots).hasSize(10);
    assertThat(slots).filteredOn("11"::equals).hasSize(2);
    assertThat(slots).filteredOn("12"::equals).hasSize(3);
    assertThat(slots).filteredOn("13"::equals).hasSize(5);
  }

  private LotteryPrize prize(Long id, long weight) {
    LotteryPrize prize = BeanUtils.instantiateClass(LotteryPrize.class);
    ReflectionTestUtils.setField(prize, "id", id);
    ReflectionTestUtils.setField(prize, "weight", weight);
    return prize;
  }
}
