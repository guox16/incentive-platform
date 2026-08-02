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

  @Column(name = "user_id", nullable = false, length = 36, updatable = false)
  private String userId;

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

  protected PointTransaction() {}

  public PointTransaction(String businessId, String userId, PointTransactionType type, long amount,
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

  public String getId() { return id; }
  public String getBusinessId() { return businessId; }
  public String getUserId() { return userId; }
  public PointTransactionType getType() { return type; }
  public long getAmount() { return amount; }
  public long getBalanceBefore() { return balanceBefore; }
  public long getBalanceAfter() { return balanceAfter; }
  public String getSource() { return source; }
  public String getRemark() { return remark; }
  public Instant getCreatedAt() { return createdAt; }
}
