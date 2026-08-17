package com.incentive.common.security;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** 黑名单不可用时拒绝令牌，避免 Redis 故障绕过撤销状态。 */
public final class RedisAccessTokenBlacklistValidator implements OAuth2TokenValidator<Jwt> {
  private static final OAuth2Error REVOKED =
      new OAuth2Error("invalid_token", "Access Token 已撤销", null);
  private static final OAuth2Error STATUS_UNAVAILABLE =
      new OAuth2Error("invalid_token", "无法确认 Access Token 撤销状态", null);
  private final StringRedisTemplate redis;

  public RedisAccessTokenBlacklistValidator(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    try {
      String tokenId = jwt.getId();
      if (tokenId == null || tokenId.isBlank()) {
        return OAuth2TokenValidatorResult.failure(REVOKED);
      }
      List<String> values = redis.opsForValue().multiGet(List.of(
          AccessTokenBlacklistKeys.jti(tokenId),
          AccessTokenBlacklistKeys.userCutoff(jwt.getSubject())));
      if (values == null || values.size() != 2) {
        return OAuth2TokenValidatorResult.failure(STATUS_UNAVAILABLE);
      }
      if (values.get(0) != null) {
        return OAuth2TokenValidatorResult.failure(REVOKED);
      }
      String cutoffValue = values.get(1);
      if (cutoffValue == null) {
        return OAuth2TokenValidatorResult.success();
      }
      long issuedAtMillis = issuedAtMillis(jwt);
      return issuedAtMillis <= Long.parseLong(cutoffValue)
          ? OAuth2TokenValidatorResult.failure(REVOKED)
          : OAuth2TokenValidatorResult.success();
    } catch (RuntimeException exception) {
      return OAuth2TokenValidatorResult.failure(STATUS_UNAVAILABLE);
    }
  }

  private long issuedAtMillis(Jwt jwt) {
    Number preciseIssuedAt = jwt.getClaim("issued_at_ms");
    if (preciseIssuedAt != null) return preciseIssuedAt.longValue();
    return jwt.getIssuedAt().toEpochMilli();
  }
}
