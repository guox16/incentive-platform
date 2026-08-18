package com.incentive.points.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.incentive.points.domain.PointAccount;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PointAccountRepositoryTest {
  @Autowired private PointAccountRepository repository;

  @Test
  void atomicallyCreditsAndDebitsBalance() {
    PointAccount account = new PointAccount(1L);
    account.credit(100);
    repository.saveAndFlush(account);

    assertThat(repository.debitAtomically(1L, 40, Instant.now())).isEqualTo(1);
    assertThat(repository.findById(1L).orElseThrow().getBalance()).isEqualTo(60);
    assertThat(repository.creditAtomically(
        1L, 10, Long.MAX_VALUE - 10, Instant.now())).isEqualTo(1);
    assertThat(repository.findById(1L).orElseThrow().getBalance()).isEqualTo(70);
  }

  @Test
  void leavesBalanceUnchangedWhenDebitIsInsufficient() {
    PointAccount account = new PointAccount(1L);
    account.credit(10);
    repository.saveAndFlush(account);

    assertThat(repository.debitAtomically(1L, 11, Instant.now())).isZero();
    assertThat(repository.findById(1L).orElseThrow().getBalance()).isEqualTo(10);
  }

  @Test
  void preventsCreditOverflow() {
    PointAccount account = new PointAccount(1L);
    account.credit(Long.MAX_VALUE);
    repository.saveAndFlush(account);

    assertThat(repository.creditAtomically(1L, 1, Long.MAX_VALUE - 1, Instant.now())).isZero();
    assertThat(repository.findById(1L).orElseThrow().getBalance())
        .isEqualTo(Long.MAX_VALUE);
  }
}
