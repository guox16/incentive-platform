package com.incentive.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.incentive.user.config.JwtProperties;
import com.incentive.user.domain.PermissionCode;
import com.incentive.user.domain.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

class JwtTokenServiceTest {
  private static final String SECRET = "test-only-jwt-secret-with-at-least-32-characters";

  @Test
  void issuesSignedTokenWithUserIdentityAndExpiry() {
    SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key));
    Clock clock = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC);
    JwtProperties properties = new JwtProperties(
        SECRET, "https://incentive.local", "incentive-api", Duration.ofMinutes(15));
    JwtTokenService service = new JwtTokenService(encoder, properties, clock);

    IssuedAccessToken issued = service.issue(42L, new AuthorizationSnapshot(
        UserRole.ADMIN, List.of(PermissionCode.ACCOUNT_SELF, PermissionCode.ACTIVITY_MANAGE)));
    var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
    var jwt = decoder.decode(issued.value());

    assertThat(jwt.getSubject()).isEqualTo("42");
    assertThat(jwt.getIssuer().toString()).isEqualTo("https://incentive.local");
    assertThat(jwt.getAudience()).containsExactly("incentive-api");
    assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
    assertThat(jwt.getClaimAsStringList("permissions"))
        .containsExactly("ACCOUNT_SELF", "ACTIVITY_MANAGE");
    assertThat(jwt.getIssuedAt()).isEqualTo(clock.instant());
    assertThat(jwt.getExpiresAt()).isEqualTo(clock.instant().plusSeconds(900));
    assertThat(issued.expiresInSeconds()).isEqualTo(900);
  }
}
