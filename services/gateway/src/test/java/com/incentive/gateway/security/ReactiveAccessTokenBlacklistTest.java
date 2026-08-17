package com.incentive.gateway.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveAccessTokenBlacklistTest {
  @Test
  void rejectsBlacklistedJti() {
    ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.multiGet(List.of(ReactiveAccessTokenBlacklist.JTI_PREFIX + "blocked",
        ReactiveAccessTokenBlacklist.USER_CUTOFF_PREFIX + "42")))
        .thenReturn(Mono.just(Arrays.asList("1", null)));

    StepVerifier.create(new ReactiveAccessTokenBlacklist(redis).validate(jwt("blocked", 1000L)))
        .expectError(JwtValidationException.class)
        .verify();
  }

  @Test
  void rejectsTokenBeforeUserCutoff() {
    ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.multiGet(List.of(ReactiveAccessTokenBlacklist.JTI_PREFIX + "old",
        ReactiveAccessTokenBlacklist.USER_CUTOFF_PREFIX + "42")))
        .thenReturn(Mono.just(Arrays.asList(null, "2000")));

    StepVerifier.create(new ReactiveAccessTokenBlacklist(redis).validate(jwt("old", 1000L)))
        .expectError(JwtValidationException.class)
        .verify();
  }

  @Test
  void failsClosedWhenRedisIsUnavailable() {
    ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.multiGet(List.of(ReactiveAccessTokenBlacklist.JTI_PREFIX + "valid",
        ReactiveAccessTokenBlacklist.USER_CUTOFF_PREFIX + "42")))
        .thenReturn(Mono.error(new IllegalStateException("redis unavailable")));

    StepVerifier.create(new ReactiveAccessTokenBlacklist(redis).validate(jwt("valid", 3000L)))
        .expectError(JwtValidationException.class)
        .verify();
  }

  private Jwt jwt(String id, long issuedAtMillis) {
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("42")
        .claim("jti", id)
        .issuedAt(Instant.ofEpochMilli(issuedAtMillis))
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("issued_at_ms", issuedAtMillis)
        .build();
  }
}
