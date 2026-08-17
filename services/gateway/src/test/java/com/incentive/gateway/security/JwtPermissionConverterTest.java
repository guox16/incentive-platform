package com.incentive.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtPermissionConverterTest {
  private final JwtPermissionConverter converter = new JwtPermissionConverter();

  @Test
  void convertsPermissionClaimsWithoutAddingRoleInheritance() {
    Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
        Map.of("alg", "HS256"), Map.of(
            "sub", "7",
            "role", "ADMIN",
            "permissions", List.of("ACTIVITY_MANAGE", "PRIZE_MANAGE")));

    assertThat(converter.convert(jwt))
        .extracting("authority")
        .containsExactly("ACTIVITY_MANAGE", "PRIZE_MANAGE");
  }

  @Test
  void tokenWithoutPermissionsReceivesNoAuthorities() {
    Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
        Map.of("alg", "HS256"), Map.of("sub", "7"));

    assertThat(converter.convert(jwt)).isEmpty();
  }
}
