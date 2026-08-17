package com.incentive.user.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incentive.common.security.AccessTokenBlacklistKeys;
import com.incentive.user.config.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AccessTokenBlacklistServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-17T06:00:00Z");
  @Mock private StringRedisTemplate redis;
  @Mock private ValueOperations<String, String> values;
  private AccessTokenBlacklistService service;

  @BeforeEach
  void setUp() {
    when(redis.opsForValue()).thenReturn(values);
    service = new AccessTokenBlacklistService(redis,
        new JwtProperties("12345678901234567890123456789012", "issuer", "audience",
            Duration.ofMinutes(15)),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void revokesOneTokenUntilItsNaturalExpiration() {
    Jwt jwt = new Jwt("token", NOW, NOW.plusSeconds(120),
        Map.of("alg", "HS256"), Map.of("sub", "7", "jti", "token-7"));

    service.revoke(jwt);

    verify(values).set(AccessTokenBlacklistKeys.jti("token-7"), "1", Duration.ofSeconds(120));
  }

  @Test
  void revokesAllPreviouslyIssuedTokensForUser() {
    service.revokeAllForUser(7L);

    verify(values).set(AccessTokenBlacklistKeys.userCutoff("7"),
        Long.toString(NOW.toEpochMilli()), Duration.ofMinutes(15));
  }
}
