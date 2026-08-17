package com.incentive.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.internal-jwt")
public record InternalJwtProperties(
    String secret, String issuer, String audience, Duration tokenTtl) {
  public InternalJwtProperties {
    if (secret == null || secret.length() < 32) {
      throw new IllegalArgumentException("security.internal-jwt.secret 至少需要 32 个字符");
    }
    if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()) {
      throw new IllegalArgumentException("内部 JWT issuer/audience 不能为空");
    }
    if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
      throw new IllegalArgumentException("security.internal-jwt.token-ttl 必须大于 0");
    }
  }
}
