package com.incentive.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens",
    uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_hash", columnNames = "token_hash"),
    indexes = {
        @Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_family", columnList = "token_family"),
        @Index(name = "idx_refresh_tokens_expires", columnList = "expires_at")
    })
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
  private String tokenHash;

  @Column(name = "token_family", nullable = false, length = 36, updatable = false)
  private String tokenFamily;

  @Column(name = "expires_at", nullable = false, updatable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected RefreshToken() {}

  public RefreshToken(Long userId, String tokenHash, String tokenFamily,
      Instant expiresAt, Instant createdAt) {
    this.userId = userId;
    this.tokenHash = tokenHash;
    this.tokenFamily = tokenFamily;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public boolean isRevoked() { return revokedAt != null; }
  public boolean isExpiredAt(Instant instant) { return !expiresAt.isAfter(instant); }
  public void revoke(Instant instant) { if (revokedAt == null) revokedAt = instant; }
  public Long getUserId() { return userId; }
  public String getTokenFamily() { return tokenFamily; }
  public Instant getRevokedAt() { return revokedAt; }
}
