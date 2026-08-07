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
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false)
  private long balance;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  /** 供 JPA 创建积分账户实体。 */
  protected PointAccount() {}

  /** 创建余额为零的用户积分账户。 */
  public PointAccount(Long userId) {
    this.userId = userId;
    this.balance = 0;
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  /** 增加积分并返回变更前余额。 */
  public long credit(long amount) {
    long before = balance;
    balance = Math.addExact(balance, amount);
    return before;
  }

  /** 扣减积分并返回变更前余额；余额不足时抛出异常。 */
  public long debit(long amount) {
    if (balance < amount) {
      throw new InsufficientPointsException();
    }
    long before = balance;
    balance -= amount;
    return before;
  }

  @PrePersist
  /** 在新增持久化前初始化创建和更新时间。 */
  void beforeInsert() {
    if (createdAt == null) createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  /** 在更新持久化前刷新更新时间。 */
  void beforeUpdate() { updatedAt = Instant.now(); }

  /** 获取账户所属用户 ID。 */
  public Long getUserId() { return userId; }
  /** 获取当前积分余额。 */
  public long getBalance() { return balance; }
  /** 获取账户创建时间。 */
  public Instant getCreatedAt() { return createdAt; }
  /** 获取账户最后更新时间。 */
  public Instant getUpdatedAt() { return updatedAt; }
}
