package com.incentive.activity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "pending_awards",
    uniqueConstraints = @UniqueConstraint(name = "uk_pending_awards_source",
        columnNames = {"source_type", "source_record_id"}),
    indexes = {
        @Index(name = "idx_pending_awards_status_created", columnList = "status,created_at"),
        @Index(name = "idx_pending_awards_user_created", columnList = "user_id,created_at")
    })
public class PendingAward {
  public enum SourceType { LOTTERY, REDEMPTION }
  public enum Status { PENDING, PROCESSING, AWARDED, FAILED }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 16, updatable = false)
  private SourceType sourceType;
  @Column(name = "source_record_id", nullable = false, updatable = false)
  private Long sourceRecordId;
  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;
  @Column(name = "prize_id", nullable = false, updatable = false)
  private Long prizeId;
  @Column(name = "prize_name_snapshot", nullable = false, length = 100, updatable = false)
  private String prizeName;
  @Enumerated(EnumType.STRING)
  @Column(name = "prize_type_snapshot", nullable = false, length = 16, updatable = false)
  private PrizeType prizeType;
  @Column(name = "award_payload_snapshot", columnDefinition = "json", updatable = false)
  private String awardPayload;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;
  @Column(name = "retry_count", nullable = false)
  private int retryCount;
  @Column(name = "last_error", length = 500)
  private String lastError;
  @Column(name = "awarded_at")
  private Instant awardedAt;
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PendingAward() {}

  public static PendingAward forLottery(LotteryParticipation participation, Instant now) {
    PendingAward award = new PendingAward();
    award.sourceType = SourceType.LOTTERY;
    award.sourceRecordId = participation.getId();
    award.userId = participation.getUserId();
    award.prizeId = participation.getPrizeId();
    award.prizeName = participation.getPrizeName();
    award.prizeType = participation.getPrizeType();
    award.awardPayload = participation.getAwardPayload();
    award.status = Status.PENDING;
    award.retryCount = 0;
    award.createdAt = now;
    award.updatedAt = now;
    return award;
  }

  public static PendingAward forRedemption(RedemptionRecord redemption, Instant now) {
    PendingAward award = new PendingAward();
    award.sourceType = SourceType.REDEMPTION;
    award.sourceRecordId = redemption.getId();
    award.userId = redemption.getUserId();
    award.prizeId = redemption.getPrizeId();
    award.prizeName = redemption.getPrizeName();
    award.prizeType = redemption.getPrizeType();
    award.awardPayload = redemption.getAwardPayload();
    award.status = Status.PENDING;
    award.retryCount = 0;
    award.createdAt = now;
    award.updatedAt = now;
    return award;
  }

  public Long getId() { return id; }
  public SourceType getSourceType() { return sourceType; }
  public Long getSourceRecordId() { return sourceRecordId; }
  public Long getUserId() { return userId; }
  public Long getPrizeId() { return prizeId; }
  public String getPrizeName() { return prizeName; }
  public PrizeType getPrizeType() { return prizeType; }
  public String getAwardPayload() { return awardPayload; }
  public Status getStatus() { return status; }
  public int getRetryCount() { return retryCount; }
  public Instant getCreatedAt() { return createdAt; }
}
