package com.incentive.award.domain;

import com.incentive.award.messaging.AwardCommandMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shop_redemptions")
public class ShopRedemption {
  public enum Status { RESERVED, PAID, COMPLETED, REJECTED }

  @Id
  private Long id;
  @Column(nullable = false, updatable = false)
  private Long userId;
  @Column(nullable = false, updatable = false, length = 64)
  private String requestId;
  @Column(nullable = false, updatable = false)
  private Long awardId;
  @Column(nullable = false, updatable = false, length = 100)
  private String awardName;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 16)
  private AwardType awardType;
  @Column(updatable = false, columnDefinition = "json")
  private String awardPayload;
  @Column(nullable = false, updatable = false)
  private long pointsCost;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;
  private Long pointTransactionId;
  private Long balanceAfter;
  @Column(nullable = false, updatable = false)
  private Instant createdAt;
  @Column(nullable = false)
  private Instant nextAttemptAt;

  protected ShopRedemption() {}

  public ShopRedemption(Long id, Long userId, String requestId, Award award, Instant now) {
    this.id = id;
    this.userId = userId;
    this.requestId = requestId;
    this.awardId = award.getId();
    this.awardName = award.getName();
    this.awardType = award.getType();
    this.awardPayload = award.getAwardPayload();
    this.pointsCost = award.getRedemptionPointsPrice();
    this.status = Status.RESERVED;
    this.createdAt = now;
    this.nextAttemptAt = now;
  }

  public void pay(Long transactionId, long balanceAfter) {
    this.pointTransactionId = transactionId;
    this.balanceAfter = balanceAfter;
    status = Status.PAID;
  }

  public void reject() { status = Status.REJECTED; }

  public void complete() { status = Status.COMPLETED; }

  public void defer(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

  public AwardCommandMessage issuanceCommand() {
    AwardSourceType source = AwardSourceType.SHOP_REDEMPTION;
    return new AwardCommandMessage(null, source + ":" + id, source, id,
        userId, awardId, awardName, awardType, awardPayload, null);
  }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public Long getAwardId() { return awardId; }
  public String getAwardName() { return awardName; }
  public long getPointsCost() { return pointsCost; }
  public Status getStatus() { return status; }
  public Long getBalanceAfter() { return balanceAfter; }
  public Instant getCreatedAt() { return createdAt; }
}
