package com.incentive.points.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** 积分账户聚合根，负责维护余额永不为负这一核心业务不变量。 */
@Entity
@Table(name = "point_accounts")
public class PointAccount {
  @Id
  @Column(name = "user_id", nullable = false, length = 36)
  private String userId;

  @Column(nullable = false)
  private long balance;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected PointAccount() {}

  public PointAccount(String userId) {
    this.userId = userId;
    this.balance = 0;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  public long credit(long amount) {
    long before = balance;
    balance = Math.addExact(balance, amount);
    return before;
  }

  public long debit(long amount) {
    if (balance < amount) {
      throw new InsufficientPointsException();
    }
    long before = balance;
    balance -= amount;
    return before;
  }

  @PrePersist
  void beforeInsert() {
    if (createdAt == null) createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void beforeUpdate() { updatedAt = Instant.now(); }

  public String getUserId() { return userId; }
  public long getBalance() { return balance; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
