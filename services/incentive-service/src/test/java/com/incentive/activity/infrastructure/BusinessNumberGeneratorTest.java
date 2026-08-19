package com.incentive.activity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BusinessNumberGeneratorTest {
  @Test
  void generatesUniquePositiveSnowflakeIds() {
    BusinessNumberGenerator generator = new BusinessNumberGenerator(7L);
    Set<Long> ids = new HashSet<>();

    for (int i = 0; i < 10_000; i++) ids.add(generator.next());

    assertThat(ids).hasSize(10_000).allMatch(id -> id > 0);
  }

  @Test
  void rejectsWorkerIdOutsideTenBitRange() {
    assertThatThrownBy(() -> new BusinessNumberGenerator(1024L))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
