package com.incentive.user.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.refresh-token")
public record RefreshTokenProperties(Duration ttl, String cookieName, boolean cookieSecure) {
  public RefreshTokenProperties {
    if (ttl == null || ttl.isZero() || ttl.isNegative()) {
      throw new IllegalArgumentException("security.refresh-token.ttl 必须大于 0");
    }
    if (cookieName == null || cookieName.isBlank()) {
      throw new IllegalArgumentException("security.refresh-token.cookie-name 不能为空");
    }
  }
}
