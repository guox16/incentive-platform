package com.incentive.points.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PointAccountTest {
  @Test
  void creditsAndDebitsWithoutBreakingBalanceInvariant() {
    PointAccount account = new PointAccount(UUID.randomUUID().toString());

    assertThat(account.credit(100)).isZero();
    assertThat(account.debit(40)).isEqualTo(100);
    assertThat(account.getBalance()).isEqualTo(60);
  }

  @Test
  void rejectsDebitThatWouldMakeBalanceNegative() {
    PointAccount account = new PointAccount(UUID.randomUUID().toString());

    assertThatThrownBy(() -> account.debit(1)).isInstanceOf(InsufficientPointsException.class);
    assertThat(account.getBalance()).isZero();
  }
}
