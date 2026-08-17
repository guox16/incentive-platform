package com.incentive.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtUserId {
  private JwtUserId() {}

  public static Long from(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
