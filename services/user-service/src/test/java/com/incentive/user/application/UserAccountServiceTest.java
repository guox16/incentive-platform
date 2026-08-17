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
import com.incentive.user.security.IssuedAccessToken;
import com.incentive.user.security.JwtTokenService;
import com.incentive.user.security.RefreshTokenService;
import com.incentive.user.security.IssuedRefreshToken;
import com.incentive.user.security.RotatedRefreshToken;
import com.incentive.user.support.UserBusinessException;
import java.time.Instant;
import java.util.Optional;
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
  @Mock private JwtTokenService jwtTokenService;
  @Mock private RefreshTokenService refreshTokenService;
  private UserAccountService service;

  @BeforeEach
  void setUp() {
    service = new UserAccountService(
        repository, new BCryptPasswordEncoder(), jwtTokenService, refreshTokenService);
  }

  @Test
  void registersAccountWithHashedPassword() {
    when(repository.existsByUsername("alice")).thenReturn(false);
    when(repository.existsByPhone("13800138000")).thenReturn(false);
    when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
      UserAccount account = invocation.getArgument(0);
      ReflectionTestUtils.setField(account, "id", 1L);
      ReflectionTestUtils.setField(account, "createdAt", Instant.now());
      ReflectionTestUtils.setField(account, "updatedAt", Instant.now());
      return account;
    });

    var response = service.register(new RegisterRequest("alice", "13800138000", "secret12", "Alice"));

    assertThat(response.username()).isEqualTo("alice");
    assertThat(response.nickname()).isEqualTo("Alice");
    assertThat(response.phone()).isEqualTo("13800138000");
  }

  @Test
  void rejectsDuplicateUsername() {
    when(repository.existsByUsername("alice")).thenReturn(true);

    assertThatThrownBy(() -> service.register(new RegisterRequest("alice", "13800138000", "secret12", "Alice")))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("USERNAME_ALREADY_EXISTS");
  }

  @Test
  void returnsProfileForCorrectPassword() {
    UserAccount account = account("alice", new BCryptPasswordEncoder().encode("secret12"));
    when(repository.findByUsernameOrPhone("alice", "alice")).thenReturn(Optional.of(account));
    when(jwtTokenService.issue(1L)).thenReturn(new IssuedAccessToken("signed-token", 900));
    when(refreshTokenService.issue(1L)).thenReturn(new IssuedRefreshToken("refresh-token", 2592000));

    var session = service.login(new LoginRequest("alice", "secret12"));
    var response = session.response();
    assertThat(response.user().username()).isEqualTo("alice");
    assertThat(response.accessToken()).isEqualTo("signed-token");
    assertThat(response.expiresIn()).isEqualTo(900);
    assertThat(session.refreshToken()).isEqualTo("refresh-token");
  }

  @Test
  void rejectsWrongPasswordWithoutLeakingAccountExistence() {
    UserAccount account = account("alice", new BCryptPasswordEncoder().encode("secret12"));
    when(repository.findByUsernameOrPhone("alice", "alice")).thenReturn(Optional.of(account));

    assertThatThrownBy(() -> service.login(new LoginRequest("alice", "wrong123")))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("INVALID_CREDENTIALS");
  }

  @Test
  void readsAndUpdatesProfile() {
    UserAccount account = account("alice", new BCryptPasswordEncoder().encode("secret12"));
    when(repository.findById(account.getId())).thenReturn(Optional.of(account));

    assertThat(service.getProfile(account.getId()).nickname()).isEqualTo("Alice");
    var updated = service.updateProfile(account.getId(), new UpdateProfileRequest("New name", "13900139000"));
    assertThat(updated.nickname()).isEqualTo("New name");
    assertThat(updated.phone()).isEqualTo("13900139000");
  }

  @Test
  void rejectsPhoneNumberAlreadyUsedByAnotherAccount() {
    UserAccount account = account("alice", new BCryptPasswordEncoder().encode("secret12"));
    when(repository.findById(account.getId())).thenReturn(Optional.of(account));
    when(repository.existsByPhone("13900139000")).thenReturn(true);

    assertThatThrownBy(() -> service.updateProfile(account.getId(), new UpdateProfileRequest("New name", "13900139000")))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("PHONE_ALREADY_EXISTS");
  }

  @Test
  void logsInWithPhoneNumber() {
    UserAccount account = account("alice", new BCryptPasswordEncoder().encode("secret12"));
    when(repository.findByUsernameOrPhone("13800138000", "13800138000")).thenReturn(Optional.of(account));
    when(jwtTokenService.issue(1L)).thenReturn(new IssuedAccessToken("signed-token", 900));
    when(refreshTokenService.issue(1L)).thenReturn(new IssuedRefreshToken("refresh-token", 2592000));

    assertThat(service.login(new LoginRequest("13800138000", "secret12"))
        .response().user().username()).isEqualTo("alice");
  }

  @Test
  void rotatesRefreshTokenAndIssuesNewAccessToken() {
    UserAccount account = account("alice", "hash");
    when(refreshTokenService.rotate("old-refresh"))
        .thenReturn(new RotatedRefreshToken(1L, "new-refresh", 2592000));
    when(repository.findById(1L)).thenReturn(Optional.of(account));
    when(jwtTokenService.issue(1L)).thenReturn(new IssuedAccessToken("new-access", 900));

    var session = service.refresh("old-refresh");

    assertThat(session.response().accessToken()).isEqualTo("new-access");
    assertThat(session.refreshToken()).isEqualTo("new-refresh");
  }

  @Test
  void reportsMissingUser() {
    Long id = 1L;
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProfile(id)).isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("USER_NOT_FOUND");
  }

  private UserAccount account(String username, String hash) {
    UserAccount account = new UserAccount(username, "13800138000", hash, "Alice");
    ReflectionTestUtils.setField(account, "id", 1L);
    ReflectionTestUtils.setField(account, "createdAt", Instant.now());
    ReflectionTestUtils.setField(account, "updatedAt", Instant.now());
    return account;
  }
}
