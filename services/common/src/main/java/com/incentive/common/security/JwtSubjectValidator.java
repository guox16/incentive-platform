package com.incentive.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtSubjectValidator implements OAuth2TokenValidator<Jwt> {
  private static final OAuth2Error INVALID_SUBJECT =
      new OAuth2Error("invalid_token", "JWT subject 必须是正整数用户 ID", null);

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    try {
      return Long.parseLong(jwt.getSubject()) > 0
          ? OAuth2TokenValidatorResult.success()
          : OAuth2TokenValidatorResult.failure(INVALID_SUBJECT);
    } catch (RuntimeException exception) {
      return OAuth2TokenValidatorResult.failure(INVALID_SUBJECT);
    }
  }
}
