package com.incentive.award.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "award_inventory_ledger")
public class AwardInventoryLedger {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "award_id", nullable = false, updatable = false)
  private Long awardId;

  @Column(name = "business_no", nullable = false, unique = true, length = 64, updatable = false)
  private String businessNo;

  @Column(name = "operation_type", nullable = false, length = 16, updatable = false)
  private String operationType;

  @Column(name = "change_amount", nullable = false, updatable = false)
  private long changeAmount;

  @Column(name = "available_after", nullable = false, updatable = false)
  private long availableAfter;

  @Column(length = 256, updatable = false)
  private String remark;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AwardInventoryLedger() {}

  public AwardInventoryLedger(Long awardId, String businessNo, long changeAmount,
      long availableAfter, String remark, Instant now) {
    this.awardId = awardId;
    this.businessNo = businessNo;
    this.operationType = changeAmount > 0 ? "INCREASE" : "DECREASE";
    this.changeAmount = changeAmount;
    this.availableAfter = availableAfter;
    this.remark = remark;
    this.createdAt = now;
  }

  public Long getId() { return id; }
  public Long getAwardId() { return awardId; }
  public String getBusinessNo() { return businessNo; }
  public String getOperationType() { return operationType; }
  public long getChangeAmount() { return changeAmount; }
  public long getAvailableAfter() { return availableAfter; }
  public String getRemark() { return remark; }
  public Instant getCreatedAt() { return createdAt; }
}
