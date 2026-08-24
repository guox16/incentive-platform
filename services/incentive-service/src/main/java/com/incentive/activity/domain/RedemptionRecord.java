package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "redemption_records",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_redemption_records_request_id", columnNames = "request_id"),
        @UniqueConstraint(name = "uk_redemption_records_point_business_id",
            columnNames = "point_business_id")
    })
public class RedemptionRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "request_id", nullable = false, updatable = false, length = 64)
  private String requestId;
  @Column(name = "activity_id", nullable = false, updatable = false)
  private Long activityId;
  @Column(name = "activity_code_snapshot", nullable = false, updatable = false, length = 64)
  private String activityCode;
  @Column(name = "rule_id", nullable = false, updatable = false)
  private Long ruleId;
  @Column(name = "rule_version", nullable = false, updatable = false)
  private int ruleVersion;
  @Column(name = "item_id", nullable = false, updatable = false)
  private Long itemId;
  @Column(name = "item_code_snapshot", nullable = false, length = 64, updatable = false)
  private String itemCode;
  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;
  @Column(name = "prize_id", nullable = false, updatable = false)
  private Long prizeId;
  @Column(name = "prize_name_snapshot", nullable = false, length = 100, updatable = false)
  private String prizeName;
  @Enumerated(EnumType.STRING)
  @Column(name = "prize_type_snapshot", nullable = false, length = 16, updatable = false)
  private PrizeType prizeType;
  @Column(name = "cover_url_snapshot", length = 500, updatable = false)
  private String coverUrl;
  @Column(name = "award_payload_snapshot", columnDefinition = "json", updatable = false)
  private String awardPayload;
  @Column(name = "points_cost", nullable = false, updatable = false)
  private long pointsCost;
  @Column(name = "eligibility_result", columnDefinition = "json", updatable = false)
  private String eligibilityResult;
  @Column(name = "point_business_id", nullable = false, updatable = false)
  private Long pointBusinessId;
  @Column(name = "point_transaction_id")
  private Long pointTransactionId;
  @Column(name = "balance_after")
  private Long balanceAfter;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private RedemptionStatus status;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected RedemptionRecord() {}

  public void complete(Long transactionId, long balanceAfter, Instant now) {
    if (status == RedemptionStatus.COMPLETED) return;
    this.pointTransactionId = transactionId;
    this.balanceAfter = balanceAfter;
    this.status = RedemptionStatus.COMPLETED;
    this.updatedAt = now;
  }

  public Long getId() { return id; }
  public String getRequestId() { return requestId; }
  public Long getActivityId() { return activityId; }
  public String getActivityCode() { return activityCode; }
  public Long getItemId() { return itemId; }
  public String getItemCode() { return itemCode; }
  public Long getUserId() { return userId; }
  public Long getPrizeId() { return prizeId; }
  public String getPrizeName() { return prizeName; }
  public PrizeType getPrizeType() { return prizeType; }
  public String getCoverUrl() { return coverUrl; }
  public String getAwardPayload() { return awardPayload; }
  public long getPointsCost() { return pointsCost; }
  public Long getPointBusinessId() { return pointBusinessId; }
  public Long getPointTransactionId() { return pointTransactionId; }
  public Long getBalanceAfter() { return balanceAfter; }
  public RedemptionStatus getStatus() { return status; }
  public Instant getCreatedAt() { return createdAt; }
}
