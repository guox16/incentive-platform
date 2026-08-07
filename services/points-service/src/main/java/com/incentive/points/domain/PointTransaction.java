package com.incentive.points.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * 积分流水是只追加的事实记录。应用层只创建和查询流水，不暴露修改或删除入口。
 */
@Entity
@Table(name = "point_transactions",
    uniqueConstraints = @UniqueConstraint(name = "uk_point_transaction_business_id", columnNames = "business_id"),
    indexes = @Index(name = "idx_point_transaction_user_time", columnList = "user_id,created_at"))
public class PointTransaction {
  @Id
  @Column(nullable = false, length = 36)
  private String id;

  @Column(name = "business_id", nullable = false, length = 64, updatable = false)
  private String businessId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16, updatable = false)
  private PointTransactionType type;

  @Column(nullable = false, updatable = false)
  private long amount;

  @Column(name = "balance_before", nullable = false, updatable = false)
  private long balanceBefore;

  @Column(name = "balance_after", nullable = false, updatable = false)
  private long balanceAfter;

  @Column(nullable = false, length = 32, updatable = false)
  private String source;

  @Column(length = 200, updatable = false)
  private String remark;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** 供 JPA 创建积分流水实体。 */
  protected PointTransaction() {}

  /** 创建一条不可修改的积分流水记录。 */
  public PointTransaction(String businessId, Long userId, PointTransactionType type, long amount,
      long balanceBefore, long balanceAfter, String source, String remark) {
    this.id = UUID.randomUUID().toString();
    this.businessId = businessId;
    this.userId = userId;
    this.type = type;
    this.amount = amount;
    this.balanceBefore = balanceBefore;
    this.balanceAfter = balanceAfter;
    this.source = source;
    this.remark = remark;
    this.createdAt = Instant.now();
  }

  /** 获取流水 ID。 */
  public String getId() { return id; }
  /** 获取业务幂等号。 */
  public String getBusinessId() { return businessId; }
  /** 获取用户 ID。 */
  public Long getUserId() { return userId; }
  /** 获取积分变动类型。 */
  public PointTransactionType getType() { return type; }
  /** 获取积分变动数量。 */
  public long getAmount() { return amount; }
  /** 获取变动前余额。 */
  public long getBalanceBefore() { return balanceBefore; }
  /** 获取变动后余额。 */
  public long getBalanceAfter() { return balanceAfter; }
  /** 获取积分来源。 */
  public String getSource() { return source; }
  /** 获取备注信息。 */
  public String getRemark() { return remark; }
  /** 获取流水创建时间。 */
  public Instant getCreatedAt() { return createdAt; }
}
