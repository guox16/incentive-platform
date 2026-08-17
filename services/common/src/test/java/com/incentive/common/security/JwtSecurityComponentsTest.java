package com.incentive.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class JwtSecurityComponentsTest {
  @Test
  void convertsOnlyExplicitPermissionsWithoutRoleInheritance() {
    Jwt jwt = jwt("42", Map.of("permissions", List.of("POINTS_SELF", "CHECK_IN")));

    assertThat(new JwtPermissionConverter().convert(jwt))
        .extracting(authority -> authority.getAuthority())
        .containsExactly("POINTS_SELF", "CHECK_IN");
  }

  @Test
  void rejectsNonNumericUserSubject() {
    var result = new JwtSubjectValidator().validate(jwt("attacker", Map.of()));

    assertThat(result.hasErrors()).isTrue();
  }

  @Test
  void validatesExpectedAudience() {
    Jwt jwt = jwt("42", Map.of("aud", List.of("incentive-api")));

    assertThat(new JwtAudienceValidator("incentive-api").validate(jwt).hasErrors()).isFalse();
    assertThat(new JwtAudienceValidator("another-api").validate(jwt).hasErrors()).isTrue();
  }

  @Test
  void rejectsJtiStoredInRedisBlacklist() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.multiGet(List.of(AccessTokenBlacklistKeys.jti("revoked-jti"),
        AccessTokenBlacklistKeys.userCutoff("42"))))
        .thenReturn(Arrays.asList("1", null));
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("42")
        .claim("jti", "revoked-jti")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();

    assertThat(new RedisAccessTokenBlacklistValidator(redis).validate(jwt).hasErrors()).isTrue();
  }

  @Test
  void rejectsTokenIssuedBeforeUserCutoff() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    when(values.multiGet(List.of(AccessTokenBlacklistKeys.jti("old-jti"),
        AccessTokenBlacklistKeys.userCutoff("42"))))
        .thenReturn(Arrays.asList(null, "2000"));
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("42")
        .claim("jti", "old-jti")
        .issuedAt(Instant.ofEpochMilli(1000))
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("issued_at_ms", 1000L)
        .build();

    assertThat(new RedisAccessTokenBlacklistValidator(redis).validate(jwt).hasErrors()).isTrue();
  }

  private Jwt jwt(String subject, Map<String, Object> claims) {
    var builder = Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60));
    claims.forEach(builder::claim);
    return builder.build();
  }
}
