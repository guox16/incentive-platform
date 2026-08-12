package com.incentive.award.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "prize_inventory_ledgers")
public class PrizeInventoryLedger {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "prize_id", nullable = false) private Long prizeId;
  @Column(name = "business_no", nullable = false, unique = true, length = 64) private String businessNo;
  @Column(name = "operation_type", nullable = false, length = 32) private String operationType;
  @Column(name = "change_amount", nullable = false) private long changeAmount;
  @Column(name = "balance_after", nullable = false) private long balanceAfter;
  @Column(length = 256) private String remark;
  @Column(nullable = false, updatable = false) private Instant createdAt;
  protected PrizeInventoryLedger() {}
  public PrizeInventoryLedger(Long prizeId, String businessNo, long changeAmount, long balanceAfter, String remark) {
    this.prizeId = prizeId; this.businessNo = businessNo; this.operationType = "MANUAL_ADJUST";
    this.changeAmount = changeAmount; this.balanceAfter = balanceAfter; this.remark = remark; this.createdAt = Instant.now();
  }
  public Long getId() { return id; } public Long getPrizeId() { return prizeId; } public String getBusinessNo() { return businessNo; }
  public String getOperationType() { return operationType; } public long getChangeAmount() { return changeAmount; }
  public long getBalanceAfter() { return balanceAfter; } public String getRemark() { return remark; } public Instant getCreatedAt() { return createdAt; }
}
