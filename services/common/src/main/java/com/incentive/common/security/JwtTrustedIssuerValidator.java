package com.incentive.common.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtTrustedIssuerValidator implements OAuth2TokenValidator<Jwt> {
  private static final OAuth2Error INVALID_ISSUER = new OAuth2Error(
      "invalid_token", "JWT issuer不受信任", null);

  private final Set<String> trustedIssuers;

  public JwtTrustedIssuerValidator(Collection<String> trustedIssuers) {
    this.trustedIssuers = trustedIssuers.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .collect(Collectors.toUnmodifiableSet());
    if (this.trustedIssuers.isEmpty()) {
      throw new IllegalArgumentException("trustedIssuers不能为空");
    }
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    String issuer = jwt.getClaimAsString("iss");
    return issuer != null && trustedIssuers.contains(issuer)
        ? OAuth2TokenValidatorResult.success()
        : OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
  }
}
