package com.incentive.user.security;

import com.incentive.common.security.AccessTokenBlacklistKeys;
import com.incentive.user.config.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenBlacklistService {
  private final StringRedisTemplate redis;
  private final JwtProperties properties;
  private final Clock clock;

  public AccessTokenBlacklistService(
      StringRedisTemplate redis, JwtProperties properties, Clock clock) {
    this.redis = redis;
    this.properties = properties;
    this.clock = clock;
  }

  /** 只撤销当前 Access Token，记录在其自然过期后自动删除。 */
  public void revoke(Jwt jwt) {
    if (jwt == null || jwt.getId() == null || jwt.getExpiresAt() == null) return;
    Duration remaining = Duration.between(clock.instant(), jwt.getExpiresAt());
    if (remaining.isNegative() || remaining.isZero()) return;
    redis.opsForValue().set(AccessTokenBlacklistKeys.jti(jwt.getId()), "1", remaining);
  }

  /** 拒绝该用户在截止时间之前签发的全部 Access Token。 */
  public void revokeAllForUser(Long userId) {
    redis.opsForValue().set(AccessTokenBlacklistKeys.userCutoff(userId.toString()),
        Long.toString(clock.instant().toEpochMilli()), properties.accessTokenTtl());
  }
}
