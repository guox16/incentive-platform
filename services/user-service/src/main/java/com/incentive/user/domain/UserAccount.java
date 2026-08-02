package com.incentive.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** 用户服务拥有的账户聚合根；密码只保存 BCrypt 哈希值。 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 32, updatable = false)
  private String username;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected UserAccount() {}

  public UserAccount(String username, String passwordHash, String nickname) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.nickname = nickname;
  }

  @PrePersist void beforeInsert() { createdAt = Instant.now(); updatedAt = createdAt; }
  @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
  public void changeNickname(String nickname) { this.nickname = nickname; }
  public UUID getId() { return id; }
  public String getUsername() { return username; }
  public String getPasswordHash() { return passwordHash; }
  public String getNickname() { return nickname; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}

