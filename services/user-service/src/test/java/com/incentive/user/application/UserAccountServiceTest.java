package com.incentive.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.incentive.user.domain.UserAccount;
import com.incentive.user.dto.LoginRequest;
import com.incentive.user.dto.RegisterRequest;
import com.incentive.user.dto.UpdateProfileRequest;
import com.incentive.user.repository.UserAccountRepository;
import com.incentive.user.support.UserBusinessException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {
  @Mock private UserAccountRepository repository;
  private UserAccountService service;

  @BeforeEach
  void setUp() { service = new UserAccountService(repository, new BCryptPasswordEncoder()); }

  @Test
  void registersAccountWithHashedPassword() {
    when(repository.existsByUsername("alice")).thenReturn(false);
    when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
      UserAccount account = invocation.getArgument(0);
      ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(account, "createdAt", Instant.now());
      ReflectionTestUtils.setField(account, "updatedAt", Instant.now());
      return account;
    });

    var response = service.register(new RegisterRequest("alice", "secret12", "Alice"));

    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.nickname()).isEqualTo("Alice");
  }

  @Test
  void rejectsDuplicateUsername() {
    when(repository.existsByUsername("alice")).thenReturn(true);

    assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "secret12", "Alice")))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("USERNAME_ALREADY_EXISTS");
  }

  @Test
  void returnsProfileForCorrectPassword() {
    UserAccount account = account("alice", new BCryptPasswordEncoder().encode("secret12"));
    when(repository.findByUsername("alice")).thenReturn(Optional.of(account));

    assertThat(service.login(new LoginRequest("alice", "secret12")).username()).isEqualTo("alice");
  }

  @Test
  void rejectsWrongPasswordWithoutLeakingAccountExistence() {
    UserAccount account = account("alice", new BCryptPasswordEncoder().encode("secret12"));
    when(repository.findByUsername("alice")).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> service.login(new LoginRequest("alice", "wrong123")))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  void readsAndUpdatesNickname() {
    UserAccount account = account("alice", "hash");
    when(repository.findById(account.getId())).thenReturn(Optional.of(account));

    assertThat(service.getProfile(account.getId()).nickname()).isEqualTo("Alice");
    assertThat(service.updateProfile(account.getId(), new UpdateProfileRequest("New name")).nickname()).isEqualTo("New name");
  }

  @Test
  void reportsMissingUser() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProfile(id)).isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("USER_NOT_FOUND");
  }

  private UserAccount account(String username, String hash) {
    UserAccount account = new UserAccount(username, hash, "Alice");
    ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(account, "createdAt", Instant.now());
    ReflectionTestUtils.setField(account, "updatedAt", Instant.now());
    return account;
  }
}

