package com.incentive.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

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
