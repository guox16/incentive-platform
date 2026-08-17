package com.incentive.user.security;

import com.incentive.user.config.RefreshTokenProperties;
import com.incentive.user.domain.RefreshToken;
import com.incentive.user.repository.RefreshTokenRepository;
import com.incentive.user.support.UserBusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 在小型接口后集中处理 Refresh Token 的生成、哈希、轮换和重放处置。 */
@Service
public class RefreshTokenService {
  private static final int TOKEN_BYTES = 32;
  private final RefreshTokenRepository repository;
  private final RefreshTokenProperties properties;
  private final SecureRandom secureRandom;
  private final Clock clock;

  public RefreshTokenService(RefreshTokenRepository repository,
      RefreshTokenProperties properties, SecureRandom secureRandom, Clock clock) {
    this.repository = repository;
    this.properties = properties;
    this.secureRandom = secureRandom;
    this.clock = clock;
  }

  @Transactional
  public IssuedRefreshToken issue(Long userId) {
    String rawToken = randomToken();
    Instant now = clock.instant();
    repository.save(new RefreshToken(userId, hash(rawToken), UUID.randomUUID().toString(),
        now.plus(properties.ttl()), now));
    return new IssuedRefreshToken(rawToken, properties.ttl().toSeconds());
  }

  @Transactional(noRollbackFor = UserBusinessException.class)
  public RotatedRefreshToken rotate(String rawToken) {
    RefreshToken current = findForUpdate(rawToken);
    Instant now = clock.instant();
    if (current.isRevoked()) {
      repository.revokeActiveFamily(current.getTokenFamily(), now);
      throw invalidToken("Refresh Token 已被重复使用，请重新登录");
    }
    if (current.isExpiredAt(now)) {
      current.revoke(now);
      throw invalidToken("Refresh Token 已过期，请重新登录");
    }

    current.revoke(now);
    String replacement = randomToken();
    repository.save(new RefreshToken(current.getUserId(), hash(replacement),
        current.getTokenFamily(), now.plus(properties.ttl()), now));
    return new RotatedRefreshToken(
        current.getUserId(), replacement, properties.ttl().toSeconds());
  }

  @Transactional
  public void revoke(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return;
    repository.findByTokenHashForUpdate(hash(rawToken))
        .ifPresent(token -> token.revoke(clock.instant()));
  }

  @Transactional
  public void revokeAllForUser(Long userId) {
    repository.revokeActiveByUserId(userId, clock.instant());
  }

  private RefreshToken findForUpdate(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) throw invalidToken("缺少 Refresh Token");
    return repository.findByTokenHashForUpdate(hash(rawToken))
        .orElseThrow(() -> invalidToken("Refresh Token 无效，请重新登录"));
  }

  private String randomToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(String token) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException("当前 JVM 不支持 SHA-256", exception);
    }
  }

  private UserBusinessException invalidToken(String message) {
    return new UserBusinessException("INVALID_REFRESH_TOKEN", message, HttpStatus.UNAUTHORIZED);
  }
}
