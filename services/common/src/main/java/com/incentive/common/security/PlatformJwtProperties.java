package com.incentive.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record PlatformJwtProperties(String secret, String issuer, String audience) {
  public PlatformJwtProperties {
    if (secret == null || secret.length() < 32) {
      throw new IllegalArgumentException("security.jwt.secret 至少需要 32 个字符");
    }
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("security.jwt.issuer 不能为空");
    }
    if (audience == null || audience.isBlank()) {
      throw new IllegalArgumentException("security.jwt.audience 不能为空");
    }
  }
}
