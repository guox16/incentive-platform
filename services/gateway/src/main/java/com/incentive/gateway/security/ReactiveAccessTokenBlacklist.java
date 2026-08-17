package com.incentive.gateway.security;

import java.util.List;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ReactiveAccessTokenBlacklist {
  static final String JTI_PREFIX = "auth:access:blacklist:jti:";
  static final String USER_CUTOFF_PREFIX = "auth:access:blacklist:user:";
  private static final OAuth2Error REVOKED =
      new OAuth2Error("invalid_token", "Access Token 已撤销", null);
  private static final OAuth2Error STATUS_UNAVAILABLE =
      new OAuth2Error("invalid_token", "无法确认 Access Token 撤销状态", null);
  private final ReactiveStringRedisTemplate redis;

  public ReactiveAccessTokenBlacklist(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
  }

  public Mono<Jwt> validate(Jwt jwt) {
    if (jwt.getId() == null || jwt.getId().isBlank()) return rejected(REVOKED);
    return redis.opsForValue().multiGet(List.of(
            JTI_PREFIX + jwt.getId(), USER_CUTOFF_PREFIX + jwt.getSubject()))
        .flatMap(values -> validateValues(jwt, values))
        .onErrorResume(JwtValidationException.class, Mono::error)
        .onErrorResume(error -> rejected(STATUS_UNAVAILABLE));
  }

  private Mono<Jwt> validateValues(Jwt jwt, List<String> values) {
    if (values.size() != 2) return rejected(STATUS_UNAVAILABLE);
    if (values.get(0) != null) return rejected(REVOKED);
    String cutoff = values.get(1);
    return cutoff != null && issuedAtMillis(jwt) <= Long.parseLong(cutoff)
        ? rejected(REVOKED) : Mono.just(jwt);
  }

  private long issuedAtMillis(Jwt jwt) {
    Number preciseIssuedAt = jwt.getClaim("issued_at_ms");
    return preciseIssuedAt != null ? preciseIssuedAt.longValue() : jwt.getIssuedAt().toEpochMilli();
  }

  private <T> Mono<T> rejected(OAuth2Error error) {
    return Mono.error(new JwtValidationException(error.getDescription(), List.of(error)));
  }
}
