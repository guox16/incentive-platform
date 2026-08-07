package com.incentive.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incentive.user.domain.UserAccount;
import com.incentive.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserAccountRepositoryTest {
  @Autowired private UserAccountRepository repository;

  @Test
  void enforcesUniqueUsernameAtDatabaseLevel() {
    repository.saveAndFlush(new UserAccount("alice", "13800138000", "first-hash", "Alice"));

    assertThatThrownBy(() -> repository.saveAndFlush(new UserAccount("alice", "13800138001", "second-hash", "Other")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
