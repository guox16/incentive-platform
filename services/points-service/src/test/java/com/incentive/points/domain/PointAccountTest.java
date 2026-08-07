package com.incentive.points.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PointAccountTest {
  @Test
  void creditsAndDebitsWithoutBreakingBalanceInvariant() {
    PointAccount account = new PointAccount(1L);

    assertThat(account.credit(100)).isZero();
    assertThat(account.debit(40)).isEqualTo(100);
    assertThat(account.getBalance()).isEqualTo(60);
  }

  @Test
  void rejectsDebitThatWouldMakeBalanceNegative() {
    PointAccount account = new PointAccount(1L);

    assertThatThrownBy(() -> account.debit(1)).isInstanceOf(InsufficientPointsException.class);
    assertThat(account.getBalance()).isZero();
  }
}
