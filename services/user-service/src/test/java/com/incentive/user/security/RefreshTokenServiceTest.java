package com.incentive.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.user.config.RefreshTokenProperties;
import com.incentive.user.domain.RefreshToken;
import com.incentive.user.repository.RefreshTokenRepository;
import com.incentive.user.support.UserBusinessException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");
  @Mock private RefreshTokenRepository repository;
  @Mock private SecureRandom secureRandom;
  private RefreshTokenService service;

  @BeforeEach
  void setUp() {
    service = new RefreshTokenService(repository,
        new RefreshTokenProperties(Duration.ofDays(30), "refresh_token", false),
        secureRandom, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void storesOnlyHashWhenIssuingToken() {
    stubRandomToken();
    IssuedRefreshToken issued = service.issue(42L);
    ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
    verify(repository).save(saved.capture());

    assertThat(issued.value()).isNotBlank();
    assertThat(ReflectionTestUtils.getField(saved.getValue(), "tokenHash"))
        .isEqualTo(RefreshTokenService.hash(issued.value()))
        .isNotEqualTo(issued.value());
    assertThat(ReflectionTestUtils.getField(saved.getValue(), "expiresAt"))
        .isEqualTo(NOW.plus(Duration.ofDays(30)));
  }

  @Test
  void rotatesValidTokenAndRevokesPreviousToken() {
    stubRandomToken();
    RefreshToken current = new RefreshToken(
        42L, RefreshTokenService.hash("old-token"), "family-1", NOW.plusSeconds(60), NOW);
    when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("old-token")))
        .thenReturn(Optional.of(current));

    RotatedRefreshToken rotated = service.rotate("old-token");

    assertThat(rotated.userId()).isEqualTo(42L);
    assertThat(rotated.value()).isNotBlank();
    assertThat(current.getRevokedAt()).isEqualTo(NOW);
    verify(repository).save(any(RefreshToken.class));
  }

  @Test
  void revokesTokenFamilyWhenRevokedTokenIsReused() {
    RefreshToken current = new RefreshToken(
        42L, RefreshTokenService.hash("old-token"), "family-1", NOW.plusSeconds(60), NOW.minusSeconds(10));
    current.revoke(NOW.minusSeconds(5));
    when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("old-token")))
        .thenReturn(Optional.of(current));

    assertThatThrownBy(() -> service.rotate("old-token"))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("INVALID_REFRESH_TOKEN");
    verify(repository).revokeActiveFamily("family-1", NOW);
  }

  @Test
  void rejectsAndRevokesExpiredToken() {
    RefreshToken current = new RefreshToken(
        42L, RefreshTokenService.hash("expired-token"), "family-1", NOW, NOW.minusSeconds(60));
    when(repository.findByTokenHashForUpdate(RefreshTokenService.hash("expired-token")))
        .thenReturn(Optional.of(current));

    assertThatThrownBy(() -> service.rotate("expired-token"))
        .isInstanceOf(UserBusinessException.class)
        .extracting("code").isEqualTo("INVALID_REFRESH_TOKEN");
    assertThat(current.getRevokedAt()).isEqualTo(NOW);
  }

  private void stubRandomToken() {
    doAnswer(invocation -> {
      byte[] bytes = invocation.getArgument(0);
      java.util.Arrays.fill(bytes, (byte) 7);
      return null;
    }).when(secureRandom).nextBytes(any(byte[].class));
  }
}
